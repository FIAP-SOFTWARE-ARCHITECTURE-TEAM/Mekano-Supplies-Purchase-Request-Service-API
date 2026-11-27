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

    @Test
    @DisplayName("permite enviar e receber requisicao nas transicoes previstas")
    void enviaERecebe() {
        RequisicaoCompra aberta = RequisicaoCompra.criar(List.of(item(PECA_A)), null);

        RequisicaoCompra enviada = aberta.enviar();
        RequisicaoCompra recebida = enviada.receber();

        assertThat(aberta.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(enviada.getStatus()).isEqualTo(StatusRequisicao.ENVIADA);
        assertThat(recebida.getStatus()).isEqualTo(StatusRequisicao.PRODUTO_RECEBIDO);
        assertThat(recebida.getId()).isEqualTo(aberta.getId());
    }

    @Test
    @DisplayName("rejeita transicoes invalidas")
    void rejeitaTransicaoInvalida() {
        RequisicaoCompra aberta = RequisicaoCompra.criar(List.of(item(PECA_A)), null);

        AppException ex = catchThrowableOfType(aberta::receber, AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("cancela requisicao aberta de estoque minimo mas bloqueia ordem de servico")
    void cancelarRespeitaMotivoEStatus() {
        RequisicaoCompra aberta = RequisicaoCompra.criar(List.of(item(PECA_A)), null);
        RequisicaoCompra ordemServico = RequisicaoCompra.reconstitute(
                UUID.randomUUID(), List.of(item(PECA_A)), StatusRequisicao.ABERTA,
                MotivoRequisicao.ORDEM_SERVICO, null, java.time.LocalDateTime.now());

        assertThat(aberta.cancelar().getStatus()).isEqualTo(StatusRequisicao.CANCELADA);
        AppException ex = catchThrowableOfType(ordemServico::cancelar, AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
    }

    private static ItemRequisicaoCompra item(UUID pecaUuid) {
        return ItemRequisicaoCompra.criar(
                pecaUuid, "PEA-001", "Filtro de Oleo", 5L, new BigDecimal("45.90"));
    }
}
