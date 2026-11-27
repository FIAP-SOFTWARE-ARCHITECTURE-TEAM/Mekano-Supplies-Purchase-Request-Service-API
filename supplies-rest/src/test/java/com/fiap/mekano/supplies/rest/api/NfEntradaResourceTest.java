package com.fiap.mekano.supplies.rest.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("NfEntradaResource")
class NfEntradaResourceTest {

    private static final String BASE_PATH = "/api/v4/nf-entrada";
    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final AtomicLong CHAVE_SEQ = new AtomicLong(1);

    @Inject
    AgroalDataSource dataSource;

    @BeforeEach
    void limparBase() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from movimentos_estoque");
            statement.executeUpdate("delete from nf_entradas");
            statement.executeUpdate("delete from itens_requisicao_compra");
            statement.executeUpdate("delete from estoque");
            statement.executeUpdate("delete from requisicoes_compra");
        }
    }

    @Test
    @DisplayName("POST registra NF ATIVA, credita estoque e grava movimentos CREDITO")
    void registrarCreditaEstoque() throws Exception {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 10, 5);
        String chave = proximaChave();

        String nfUuid = given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, chave, "459.00"))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(BASE_PATH))
                .body("uuid", notNullValue())
                .body("status", equalTo("ATIVA"))
                .body("chaveAcesso", equalTo(chave))
                .body("itens.size()", equalTo(2))
                .body("estoque.size()", equalTo(2))
                .extract().path("uuid");

        assertThat(saldo(PECA_A)).isEqualTo(10);
        assertThat(saldo(PECA_B)).isEqualTo(5);
        assertThat(statusNf(UUID.fromString(nfUuid))).isEqualTo("ATIVA");
        assertThat(idempotencyKeys()).containsExactlyInAnyOrder(
                "nf:%s:%s:CREDITO".formatted(nfUuid, PECA_A),
                "nf:%s:%s:CREDITO".formatted(nfUuid, PECA_B));
    }

    @Test
    @DisplayName("POST retorna 400 para chave com menos de 44 digitos")
    void registrarChaveInvalida() {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 1);

        given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, "123", "10.00"))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST retorna 400 para valor menor ou igual a zero")
    void registrarValorInvalido() {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 1);

        given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, proximaChave(), "0"))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST retorna 409 quando a requisicao nao esta PRODUTO_RECEBIDO")
    void registrarRequisicaoInvalida() {
        UUID requisicao = criarRequisicao("ABERTA", 1);

        given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, proximaChave(), "10.00"))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST retorna 409 para chave de acesso duplicada")
    void registrarChaveDuplicada() {
        UUID requisicaoA = criarRequisicao("PRODUTO_RECEBIDO", 1);
        UUID requisicaoB = criarRequisicao("PRODUTO_RECEBIDO", 2);
        String chave = proximaChave();

        given()
                .contentType(ContentType.JSON)
                .body(payload(requisicaoA, chave, "10.00"))
                .when().post(BASE_PATH).then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(payload(requisicaoB, chave, "20.00"))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("GET lista com filtros e busca NF por UUID")
    void listarEBuscar() {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 3);
        String chave = proximaChave();
        String uuid = given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, chave, "30.00"))
                .when().post(BASE_PATH).then().statusCode(201).extract().path("uuid");

        given()
                .queryParam("status", "ATIVA")
                .queryParam("requisicaoUuid", requisicao.toString())
                .queryParam("chaveAcesso", chave)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].uuid", equalTo(uuid));

        given()
                .when()
                .get(BASE_PATH + "/" + uuid)
                .then()
                .statusCode(200)
                .body("uuid", equalTo(uuid))
                .body("itens.size()", equalTo(1));
    }

    @Test
    @DisplayName("GET retorna 400 para filtro de status invalido")
    void listarFiltroInvalido() {
        given()
                .queryParam("status", "INVALIDO")
                .when().get(BASE_PATH).then().statusCode(400);
    }

    @Test
    @DisplayName("GET retorna 404 para NF inexistente")
    void buscarInexistente() {
        given()
                .when().get(BASE_PATH + "/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    @DisplayName("POST cancela NF ATIVA, estorna estoque e grava movimentos ESTORNO")
    void cancelaComEstorno() throws Exception {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 10, 5);
        String uuid = given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, proximaChave(), "459.00"))
                .when().post(BASE_PATH).then().statusCode(201).extract().path("uuid");

        given()
                .contentType(ContentType.JSON)
                .body("{\"motivo\":\"Nota emitida em duplicidade\"}")
                .when()
                .post(BASE_PATH + "/" + uuid + "/cancelar")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"))
                .body("motivoCancelamento", equalTo("Nota emitida em duplicidade"))
                .body("dataCancelamento", notNullValue());

        assertThat(saldo(PECA_A)).isZero();
        assertThat(saldo(PECA_B)).isZero();
        assertThat(statusNf(UUID.fromString(uuid))).isEqualTo("CANCELADA");
        assertThat(idempotencyKeys()).containsExactlyInAnyOrder(
                "nf:%s:%s:CREDITO".formatted(uuid, PECA_A),
                "nf:%s:%s:CREDITO".formatted(uuid, PECA_B),
                "nf:%s:%s:ESTORNO".formatted(uuid, PECA_A),
                "nf:%s:%s:ESTORNO".formatted(uuid, PECA_B));
    }

    @Test
    @DisplayName("POST retorna 409 e mantem NF/estoque quando saldo e insuficiente no estorno")
    void cancelaSemSaldoFazRollback() throws Exception {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 10);
        String uuid = given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, proximaChave(), "100.00"))
                .when().post(BASE_PATH).then().statusCode(201).extract().path("uuid");

        executar("update estoque set saldo_atual = 2 where peca_uuid = ?", PECA_A);

        given()
                .contentType(ContentType.JSON)
                .body("{\"motivo\":\"Estorno\"}")
                .when()
                .post(BASE_PATH + "/" + uuid + "/cancelar")
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));

        assertThat(saldo(PECA_A)).isEqualTo(2);
        assertThat(statusNf(UUID.fromString(uuid))).isEqualTo("ATIVA");
        assertThat(idempotencyKeys()).containsExactly(
                "nf:%s:%s:CREDITO".formatted(uuid, PECA_A));
    }

    @Test
    @DisplayName("POST retorna 409 ao cancelar NF ja cancelada")
    void cancelaDuplicado() {
        UUID requisicao = criarRequisicao("PRODUTO_RECEBIDO", 1);
        String uuid = given()
                .contentType(ContentType.JSON)
                .body(payload(requisicao, proximaChave(), "10.00"))
                .when().post(BASE_PATH).then().statusCode(201).extract().path("uuid");

        given().contentType(ContentType.JSON).body("{\"motivo\":\"Primeiro\"}")
                .when().post(BASE_PATH + "/" + uuid + "/cancelar").then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body("{\"motivo\":\"Segundo\"}")
                .when()
                .post(BASE_PATH + "/" + uuid + "/cancelar")
                .then()
                .statusCode(409);
    }

    private static String proximaChave() {
        return "%044d".formatted(CHAVE_SEQ.getAndIncrement());
    }

    private static String payload(UUID requisicao, String chave, String valor) {
        return """
                {
                  "requisicaoUuid": "%s",
                  "chaveAcesso": "%s",
                  "valorTotal": %s
                }
                """.formatted(requisicao, chave, valor);
    }

    private UUID criarRequisicao(String status, long... quantidades) {
        UUID uuid = UUID.randomUUID();
        executar("""
                insert into requisicoes_compra (uuid, status, motivo, observacao, created_at)
                values (?, ?, 'ESTOQUE_MINIMO', 'Teste NF', CURRENT_TIMESTAMP)
                """, uuid.toString(), status);

        UUID[] pecas = {PECA_A, PECA_B};
        for (int i = 0; i < quantidades.length; i++) {
            executar("""
                    insert into itens_requisicao_compra
                        (uuid, requisicao_uuid, peca_uuid, peca_codigo, peca_descricao,
                         quantidade, valor_unitario, created_at)
                    values (?, ?, ?, ?, ?, ?, 10.00, CURRENT_TIMESTAMP)
                    """, UUID.randomUUID().toString(), uuid.toString(), pecas[i].toString(),
                    "PEA-00" + (i + 1), "Peca " + (i + 1), quantidades[i]);
        }
        return uuid;
    }

    private void executar(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao executar SQL de teste: " + sql, e);
        }
    }

    private long saldo(UUID pecaUuid) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select saldo_atual from estoque where peca_uuid = ?")) {
            statement.setObject(1, pecaUuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }

    private String statusNf(UUID uuid) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select status from nf_entradas where uuid = ?")) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private java.util.List<String> idempotencyKeys() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "select idempotency_key from movimentos_estoque order by id")) {
            java.util.List<String> keys = new java.util.ArrayList<>();
            while (result.next()) {
                keys.add(result.getString(1));
            }
            return keys;
        }
    }
}
