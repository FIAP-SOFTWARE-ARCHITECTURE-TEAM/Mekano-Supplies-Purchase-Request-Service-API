# ADR-001 - Stack Java 17 + Quarkus 3.36.0 + Maven multi-modulo

- **Status:** Aceito
- **Data:** 2026-11-27
- **Contexto:** Sprint 0/Sprint 1 do Mekano Supplies Purchase Request Service API

## Contexto

O projeto Mekano foi separado em servicos. O Supplies concentra estoque,
requisicao de compra e NF de entrada. Era necessario escolher uma stack
compativel com o monolito existente (`Mekano-API`) e com o Registry, mantendo
produtividade, build reprodutivel e baixo custo operacional.

## Decisao

- **Java 17** (LTS), compilado com `maven.compiler.release=17`.
- **Quarkus 3.36.0** como runtime e framework de aplicacao.
- **Maven multi-modulo** com os modulos:
  - `supplies-domain`
  - `supplies-application`
  - `supplies-infrastructure`
  - `supplies-rest` (aplicacao Quarkus)
- **Maven Wrapper** (`mvnw` / `mvnw.cmd`) para nao depender de Maven instalado.
- Pacotes base `com.fiap.mekano.supplies.*`.
- Lombok para reducao de boilerplate e MapStruct para mapeamentos.
- Jandex nos modulos de dominio/aplicacao/infraestrutura para descoberta CDI.
- Testes com JUnit 5, AssertJ, Mockito, RestAssured e WireMock.
- JaCoCo configurado no POM pai.

## Consequencias

- Build unico (`mvn -B verify`) valida todos os modulos.
- A separacao em modulos preserva a regra de dependencia da Clean Architecture:
  `rest -> application -> domain` e `infrastructure -> domain`.
- A aplicacao Quarkus fica isolada em `supplies-rest`, permitindo empacotar
  apenas esse modulo no container.
- A stack e a mesma do monolito, reduzindo curva de aprendizado e facilitando
  a migracao dos testes e padroes existentes.

## Alternativas consideradas

- **Spring Boot:** descartado para manter consistencia com os demais servicos
  Mekano e com o material da Fase 4.
- **Modulo unico:** descartado por dificultar a fiscalizacao das dependencias
  entre dominio, aplicacao e infraestrutura.
