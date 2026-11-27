package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class RequisicaoCompraTest {

    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    @DisplayName("cria requisicao publica sempre ABERTA e com motivo ESTOQUE_MINIMO")
    void criaRequisicaoPublicaAberta() {
        RequisicaoCompra requisicao = RequisicaoCompra.criar(
                List.of(item(PECA_A), item(PECA_B)), "Reposicao de estoque");

        assertThat(requisicao.getId()).isNotNull();
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(requisicao.getMotivo()).isEqualTo(MotivoRequisicao.ESTOQUE_MINIMO);
        assertThat(requisicao.getObservacao()).isEqualTo("Reposicao de estoque");
        assertThat(requisicao.getItens()).hasSize(2);
        assertThat(requisicao.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejeita requisicao sem itens")
    void rejeitaSemItens() {
        AppException ex = catchThrowableOfType(
                () -> RequisicaoCompra.criar(List.of(), null), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejeita peca duplicada na requisicao")
    void rejeitaPecaDuplicada() {
        AppException ex = catchThrowableOfType(
                () -> RequisicaoCompra.criar(List.of(item(PECA_A), item(PECA_A)), null),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("reconstitute preserva UUID, status, motivo e data")
    void reconstitutePreservaDados() {
        UUID id = UUID.randomUUID();
        var itens = List.of(item(PECA_A));

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                id, itens, StatusRequisicao.ENVIADA, MotivoRequisicao.ORDEM_SERVICO,
                "observacao", java.time.LocalDateTime.of(2026, 11, 26, 10, 0));

        assertThat(requisicao.getId()).isEqualTo(id);
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.ENVIADA);
        assertThat(requisicao.getMotivo()).isEqualTo(MotivoRequisicao.ORDEM_SERVICO);
        assertThat(requisicao.getItens()).hasSize(1);
        assertThat(requisicao.getCreatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 11, 26, 10, 0));
    }

    private static ItemRequisicaoCompra item(UUID pecaUuid) {
        return ItemRequisicaoCompra.criar(
                pecaUuid, "PEA-001", "Filtro de Oleo", 5L, new BigDecimal("45.90"));
    }
}
