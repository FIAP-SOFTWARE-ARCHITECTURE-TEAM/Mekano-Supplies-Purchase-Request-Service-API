package com.fiap.mekano.supplies.infrastructure.client;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@QuarkusTest
@QuarkusTestResource(WireMockRegistryTestResource.class)
@DisplayName("RegistryPecaCatalogAdapter (contrato WireMock)")
class RegistryPecaCatalogAdapterTest {

    private static final String BATCH_PATH = "/api/v4/internal/pecas/batch";

    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PECA_INEXISTENTE = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Inject
    PecaCatalogPort adapter;

    @BeforeEach
    void resetWireMock() {
        WireMockRegistryTestResource.server().resetAll();
    }

    @Test
    @DisplayName("envia lote e mapeia pecas encontradas e nao encontradas")
    void buscaLoteComSucesso() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .withRequestBody(matchingJsonPath("$.pecaUuids[0]", equalTo(PECA_A.toString())))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                          "pecas": [
                                            {
                                              "uuid": "%s",
                                              "codigo": "PEA-001",
                                              "descricao": "Filtro de Oleo",
                                              "valorUnitario": 45.90,
                                              "isActive": true
                                            },
                                            {
                                              "uuid": "%s",
                                              "codigo": "PEA-002",
                                              "descricao": "Filtro de Ar",
                                              "valorUnitario": 25.00,
                                              "isActive": false
                                            }
                                          ],
                                          "naoEncontradas": ["%s"]
                                        }
                                        """.formatted(PECA_A, PECA_B, PECA_INEXISTENTE))));

        PecaCatalogResult resultado = adapter.buscarPorUuids(List.of(PECA_A, PECA_B, PECA_INEXISTENTE));

        assertThat(resultado.pecas()).hasSize(2);
        PecaCatalogItem primeira = resultado.pecas().get(0);
        assertThat(primeira.uuid()).isEqualTo(PECA_A);
        assertThat(primeira.codigo()).isEqualTo("PEA-001");
        assertThat(primeira.descricao()).isEqualTo("Filtro de Oleo");
        assertThat(primeira.valorUnitario()).isEqualByComparingTo("45.90");
        assertThat(primeira.ativo()).isTrue();

        assertThat(resultado.pecas().get(1).ativo()).isFalse();
        assertThat(resultado.naoEncontradas()).containsExactly(PECA_INEXISTENTE);

        WireMockRegistryTestResource.server().verify(
                postRequestedFor(urlEqualTo(BATCH_PATH))
                        .withRequestBody(matchingJsonPath("$.pecaUuids[2]", equalTo(PECA_INEXISTENTE.toString()))));
    }

    @Test
    @DisplayName("retorna resultado vazio sem chamar o Registry quando o lote e vazio")
    void loteVazioNaoChamaRegistry() {
        PecaCatalogResult resultado = adapter.buscarPorUuids(List.of());

        assertThat(resultado.pecas()).isEmpty();
        assertThat(resultado.naoEncontradas()).isEmpty();
        WireMockRegistryTestResource.server().verify(0,
                postRequestedFor(urlEqualTo(BATCH_PATH)));
    }

    @Test
    @DisplayName("rejeita lista nula antes de chamar o Registry")
    void listaNulaVira400() {
        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(null),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
        WireMockRegistryTestResource.server().verify(0,
                postRequestedFor(urlEqualTo(BATCH_PATH)));
    }

    @Test
    @DisplayName("rejeita resposta sem listas")
    void respostaSemListasVira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{}")));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("rejeita resposta que nao contabiliza todos os UUIDs solicitados")
    void respostaIncompletaVira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {"pecas": [], "naoEncontradas": []}
                                        """)));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("rejeita peca sem campos obrigatorios no contrato")
    void pecaSemCamposObrigatoriosVira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                          "pecas": [{"uuid": "%s"}],
                                          "naoEncontradas": []
                                        }
                                        """.formatted(PECA_A))));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("converte erro 5xx do Registry em 503")
    void registryErro500Vira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse().withStatus(500)));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("converte timeout do Registry em 503")
    void registryTimeoutVira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withFixedDelay(1500)
                                .withBody("""
                                        {"pecas": [], "naoEncontradas": []}
                                        """)));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("converte corpo vazio em 503")
    void corpoVazioVira503() {
        WireMockRegistryTestResource.server().stubFor(
                post(urlEqualTo(BATCH_PATH))
                        .willReturn(aResponse().withStatus(204)));

        AppException ex = catchThrowableOfType(
                () -> adapter.buscarPorUuids(List.of(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
    }
}
