package com.fiap.mekano.supplies.rest.api;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("RequisicaoCompraResource")
class RequisicaoCompraResourceTest {

    private static final String BASE_PATH = "/api/v4/requisicoes-compra";

    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @InjectMock
    PecaCatalogPort pecaCatalog;

    @Inject
    AgroalDataSource dataSource;

    @BeforeEach
    void limparBaseECatalogar() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from itens_requisicao_compra");
            statement.executeUpdate("delete from requisicoes_compra");
        }

        when(pecaCatalog.buscarPorUuids(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> uuids = invocation.getArgument(0);
            List<PecaCatalogItem> pecas = uuids.stream()
                    .map(uuid -> new PecaCatalogItem(
                            uuid, "PEA-001", "Filtro de Oleo", new BigDecimal("45.90"), true))
                    .toList();
            return new PecaCatalogResult(pecas, List.of());
        });
    }

    @Test
    @DisplayName("POST cria requisicao com snapshots e retorna 201 com Location")
    void criarRetorna201ComSnapshots() {
        String uuid = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "itens": [
                            {"pecaUuid": "%s", "quantidade": 5},
                            {"pecaUuid": "%s", "quantidade": 2}
                          ],
                          "observacao": "Reposicao de estoque"
                        }
                        """.formatted(PECA_A, PECA_B))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(BASE_PATH))
                .body("uuid", notNullValue())
                .body("status", equalTo("ABERTA"))
                .body("motivo", equalTo("ESTOQUE_MINIMO"))
                .body("observacao", equalTo("Reposicao de estoque"))
                .body("itens.size()", equalTo(2))
                .body("itens[0].pecaCodigo", equalTo("PEA-001"))
                .body("itens[0].pecaDescricao", equalTo("Filtro de Oleo"))
                .body("itens[0].quantidade", equalTo(5))
                .body("itens[0].valorUnitario", equalTo(45.90f))
                .extract().path("uuid");

        given()
                .when()
                .get(BASE_PATH + "/" + uuid)
                .then()
                .statusCode(200)
                .body("uuid", equalTo(uuid))
                .body("status", equalTo("ABERTA"))
                .body("itens.size()", equalTo(2));
    }

    @Test
    @DisplayName("POST sem itens retorna 400")
    void criarSemItensRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com lista vazia retorna 400")
    void criarListaVaziaRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": []}
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com quantidade invalida retorna 400")
    void criarQuantidadeInvalidaRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": [{"pecaUuid": "%s", "quantidade": 0}]}
                        """.formatted(PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com peca duplicada retorna 400")
    void criarPecaDuplicadaRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "itens": [
                            {"pecaUuid": "%s", "quantidade": 1},
                            {"pecaUuid": "%s", "quantidade": 2}
                          ]
                        }
                        """.formatted(PECA_A, PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com motivo diferente de ESTOQUE_MINIMO retorna 400")
    void criarMotivoInvalidoRetorna400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "motivo": "ORDEM_SERVICO",
                          "itens": [{"pecaUuid": "%s", "quantidade": 1}]
                        }
                        """.formatted(PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com peca inexistente no Registry retorna 404")
    void criarPecaInexistenteRetorna404() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenReturn(new PecaCatalogResult(List.of(), List.of(PECA_A)));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": [{"pecaUuid": "%s", "quantidade": 1}]}
                        """.formatted(PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com peca inativa retorna 409")
    void criarPecaInativaRetorna409() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenReturn(new PecaCatalogResult(
                        List.of(new PecaCatalogItem(PECA_A, "PEA-001", "Filtro",
                                new BigDecimal("45.90"), false)),
                        List.of()));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": [{"pecaUuid": "%s", "quantidade": 1}]}
                        """.formatted(PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("POST com Registry indisponivel retorna 503")
    void criarRegistryIndisponivelRetorna503() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenThrow(new AppException(503, "Registry indisponivel"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": [{"pecaUuid": "%s", "quantidade": 1}]}
                        """.formatted(PECA_A))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(503)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("GET lista com paginacao")
    void listarComPaginacao() {
        criarRequisicao(PECA_A);
        criarRequisicao(PECA_B);

        given()
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(1))
                .body("totalElements", equalTo(2))
                .body("totalPages", equalTo(2));
    }

    @Test
    @DisplayName("GET lista aplica filtros de status, motivo e peca")
    void listarComFiltros() {
        criarRequisicao(PECA_A);

        given()
                .queryParam("status", "ABERTA")
                .queryParam("motivo", "ESTOQUE_MINIMO")
                .queryParam("pecaUuid", PECA_A.toString())
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("totalElements", equalTo(1))
                .body("content[0].itens[0].pecaUuid", equalTo(PECA_A.toString()));
    }

    @Test
    @DisplayName("GET com filtro invalido retorna 400")
    void listarComFiltroInvalidoRetorna400() {
        given()
                .queryParam("status", "INVALIDO")
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(400)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @DisplayName("GET por UUID inexistente retorna 404")
    void buscarInexistenteRetorna404() {
        given()
                .when()
                .get(BASE_PATH + "/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    private void criarRequisicao(UUID pecaUuid) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"itens": [{"pecaUuid": "%s", "quantidade": 1}]}
                        """.formatted(pecaUuid))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(201);
    }
}
