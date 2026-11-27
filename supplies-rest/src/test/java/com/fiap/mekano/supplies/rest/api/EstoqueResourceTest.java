package com.fiap.mekano.supplies.rest.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("EstoqueResource")
class EstoqueResourceTest {

    private static final String BASE_PATH = "/api/v4/internal/estoque";
    private static final UUID PECA_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_PECA_UUID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Inject
    AgroalDataSource dataSource;

    @BeforeEach
    void limparEstoque() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from movimentos_estoque");
            statement.executeUpdate("delete from estoque");
        }
    }

    @Test
    @DisplayName("POST registra credito e GET consulta saldo")
    void creditaEConsultaSaldo() {
        given()
                .contentType(ContentType.JSON)
                .body(movimentoJson("CREDITO", 10, "credito-001"))
                .when()
                .post(BASE_PATH + "/movimentos")
                .then()
                .statusCode(201)
                .body("pecaUuid", equalTo(PECA_UUID.toString()))
                .body("saldoAtualApos", equalTo(10))
                .body("saldoReservadoApos", equalTo(0))
                .body("idempotente", equalTo(false));

        given()
                .when()
                .get(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(200)
                .body("uuid", notNullValue())
                .body("saldoAtual", equalTo(10))
                .body("disponivel", equalTo(10));
    }

    @Test
    @DisplayName("consulta inicializa estoque sem movimentos")
    void consultarCriaEstoqueQuandoInexistente() {
        given().when().get(BASE_PATH + "/" + PECA_UUID)
                .then()
                .statusCode(200)
                .body("uuid", notNullValue())
                .body("saldoAtual", equalTo(0))
                .body("saldoReservado", equalTo(0))
                .body("disponivel", equalTo(0));
    }

    @Test
    @DisplayName("repeticao da chave nao altera saldo e chave com outra operacao conflita")
    void idempotenciaEConflito() {
        String credito = movimentoJson("CREDITO", 10, "credito-duplicado");
        given().contentType(ContentType.JSON).body(credito).post(BASE_PATH + "/movimentos")
                .then().statusCode(201);
        given().contentType(ContentType.JSON).body(credito).post(BASE_PATH + "/movimentos")
                .then()
                .statusCode(200)
                .body("idempotente", equalTo(true))
                .body("saldoAtualApos", equalTo(10));
        given()
                .contentType(ContentType.JSON)
                .body(movimentoJson("CREDITO", 5, "credito-duplicado"))
                .when()
                .post(BASE_PATH + "/movimentos")
                .then()
                .statusCode(409);

        given().when().get(BASE_PATH + "/" + PECA_UUID)
                .then().statusCode(200).body("saldoAtual", equalTo(10));
    }

    @Test
    @DisplayName("chave compartilhada em pecas diferentes retorna conflito sem credito parcial")
    void chaveConcorrenteEmPecasDiferentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            var primeiro = executor.submit(() -> postarCredito(
                    PECA_UUID, "credito-compartilhado", prontos, iniciar));
            var segundo = executor.submit(() -> postarCredito(
                    OUTRA_PECA_UUID, "credito-compartilhado", prontos, iniciar));
            assertThat(prontos.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            List<Integer> statuses = List.of(
                    primeiro.get(10, TimeUnit.SECONDS),
                    segundo.get(10, TimeUnit.SECONDS));

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            int saldoPrimeiraPeca = given().when().get(BASE_PATH + "/" + PECA_UUID)
                    .then().statusCode(200).extract().path("saldoAtual");
            int saldoSegundaPeca = given().when().get(BASE_PATH + "/" + OUTRA_PECA_UUID)
                    .then().statusCode(200).extract().path("saldoAtual");
            assertThat(List.of(saldoPrimeiraPeca, saldoSegundaPeca))
                    .containsExactlyInAnyOrder(0, 5);
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("debito sem saldo retorna 409 sem alterar estoque")
    void debitoSemSaldo() {
        given().contentType(ContentType.JSON)
                .body(movimentoJson("CREDITO", 5, "credito-estoque"))
                .post(BASE_PATH + "/movimentos")
                .then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(movimentoJson("DEBITO", 6, "debito-excedente"))
                .when()
                .post(BASE_PATH + "/movimentos")
                .then()
                .statusCode(409);

        given().when().get(BASE_PATH + "/" + PECA_UUID)
                .then().statusCode(200).body("saldoAtual", equalTo(5));
    }

    @Test
    @DisplayName("lista estoque disponivel abaixo do minimo")
    void listarAbaixoMinimo() throws Exception {
        given().contentType(ContentType.JSON)
                .body(movimentoJson("CREDITO", 5, "credito-minimo"))
                .post(BASE_PATH + "/movimentos")
                .then().statusCode(201);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("update estoque set estoque_minimo = 5 where peca_uuid = '"
                    + PECA_UUID + "'");
        }

        given()
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get(BASE_PATH + "/abaixo-minimo")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].disponivel", equalTo(5))
                .body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("serializa debitos concorrentes para evitar lost update")
    void debitosConcorrentesNaoCausamLostUpdate() throws Exception {
        given().contentType(ContentType.JSON)
                .body(movimentoJson("CREDITO", 10, "credito-concorrente"))
                .post(BASE_PATH + "/movimentos")
                .then().statusCode(201);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontos = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            var primeiro = executor.submit(() -> postarDebito("debito-concorrente-1", prontos, iniciar));
            var segundo = executor.submit(() -> postarDebito("debito-concorrente-2", prontos, iniciar));
            assertThat(prontos.await(5, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            List<Integer> statuses = List.of(
                    primeiro.get(10, TimeUnit.SECONDS),
                    segundo.get(10, TimeUnit.SECONDS));

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            given().when().get(BASE_PATH + "/" + PECA_UUID)
                    .then().statusCode(200).body("saldoAtual", equalTo(3));
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    private static int postarDebito(String key, CountDownLatch prontos, CountDownLatch iniciar) throws Exception {
        prontos.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Debito concorrente nao foi liberado.");
        }
        return postarMovimento("DEBITO", 7, key, PECA_UUID);
    }

    private static int postarCredito(UUID pecaUuid, String key, CountDownLatch prontos,
                                     CountDownLatch iniciar) throws Exception {
        prontos.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Credito concorrente nao foi liberado.");
        }
        return postarMovimento("CREDITO", 5, key, pecaUuid);
    }

    private static int postarMovimento(String tipo, long quantidade, String key, UUID pecaUuid) {
        return given()
                .contentType(ContentType.JSON)
                .body(movimentoJson(tipo, quantidade, key, pecaUuid))
                .when()
                .post(BASE_PATH + "/movimentos")
                .statusCode();
    }

    private static String movimentoJson(String tipo, long quantidade, String idempotencyKey) {
        return movimentoJson(tipo, quantidade, idempotencyKey, PECA_UUID);
    }

    private static String movimentoJson(String tipo, long quantidade, String idempotencyKey, UUID pecaUuid) {
        return """
                {
                  "pecaUuid": "%s",
                  "tipo": "%s",
                  "quantidade": %d,
                  "origem": "AJUSTE",
                  "idempotencyKey": "%s"
                }
                """.formatted(pecaUuid, tipo, quantidade, idempotencyKey);
    }
}
