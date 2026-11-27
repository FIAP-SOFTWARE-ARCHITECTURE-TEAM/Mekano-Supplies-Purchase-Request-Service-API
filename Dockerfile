# --- Stage 1: Build ---
FROM registry.access.redhat.com/ubi9/openjdk-17:1.24 AS build
USER root
RUN microdnf install -y unzip && microdnf clean all
WORKDIR /workspace

# Apenas wrapper + POMs primeiro (cache de dependências)
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY supplies-domain/pom.xml supplies-domain/pom.xml
COPY supplies-application/pom.xml supplies-application/pom.xml
COPY supplies-infrastructure/pom.xml supplies-infrastructure/pom.xml
COPY supplies-rest/pom.xml supplies-rest/pom.xml

# Converte CRLF → LF (Windows), ajusta permissões e dá posse ao UID 185
RUN find . \( -name "mvnw" -o -name "*.sh" -o -name "maven-wrapper.properties" \) -exec sed -i 's/\r$//' {} + \
    && chmod +x mvnw \
    && chown -R 185:0 /workspace

USER 185
RUN ./mvnw -B -ntp dependency:go-offline -DskipTests || true

# Código-fonte
COPY --chown=185:0 supplies-domain supplies-domain
COPY --chown=185:0 supplies-application supplies-application
COPY --chown=185:0 supplies-infrastructure supplies-infrastructure
COPY --chown=185:0 supplies-rest supplies-rest

# Sem testes no build da imagem (rodar no CI)
RUN ./mvnw -B -ntp package -pl supplies-rest -am -DskipTests

# --- Stage 2: Runtime ---
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24

ENV LANGUAGE='en_US:en'

COPY --from=build --chown=185:0 /workspace/supplies-rest/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185:0 /workspace/supplies-rest/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185:0 /workspace/supplies-rest/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185:0 /workspace/supplies-rest/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8081
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8081 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]