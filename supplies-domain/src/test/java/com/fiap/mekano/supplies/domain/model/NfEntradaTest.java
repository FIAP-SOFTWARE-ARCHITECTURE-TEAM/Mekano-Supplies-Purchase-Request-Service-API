package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class NfEntradaTest {

    private static final String CHAVE_VALIDA = "35200612345678000190550000001234567890123456";
    private static final UUID REQUISICAO_UUID = UUID.randomUUID();

    @Test
    @DisplayName("cria NF ATIVA com chave de 44 digitos e valor positivo")
    void criaNfAtiva() {
        NfEntrada nf = NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("459.00"), REQUISICAO_UUID);

        assertThat(nf.getId()).isNotNull();
        assertThat(nf.getChaveAcesso()).isEqualTo(CHAVE_VALIDA);
        assertThat(nf.getValorTotal()).isEqualByComparingTo("459.00");
        assertThat(nf.getRequisicaoUuid()).isEqualTo(REQUISICAO_UUID);
        assertThat(nf.getStatus()).isEqualTo(StatusNfEntrada.ATIVA);
        assertThat(nf.getCreatedAt()).isNotNull();
        assertThat(nf.getMotivoCancelamento()).isNull();
        assertThat(nf.getDataCancelamento()).isNull();
    }

    @Test
    @DisplayName("rejeita chave de acesso invalida")
    void rejeitaChaveInvalida() {
        AppException ex = catchThrowableOfType(
                () -> NfEntrada.criar("123", new BigDecimal("10"), REQUISICAO_UUID),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejeita chave de acesso vazia e requisicao nula")
    void rejeitaCamposObrigatorios() {
        AppException chave = catchThrowableOfType(
                () -> NfEntrada.criar(" ", new BigDecimal("10"), REQUISICAO_UUID),
                AppException.class);
        AppException requisicao = catchThrowableOfType(
                () -> NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("10"), null),
                AppException.class);

        assertThat(chave.getStatus()).isEqualTo(400);
        assertThat(requisicao.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejeita valor total menor ou igual a zero")
    void rejeitaValorInvalido() {
        AppException zero = catchThrowableOfType(
                () -> NfEntrada.criar(CHAVE_VALIDA, BigDecimal.ZERO, REQUISICAO_UUID),
                AppException.class);
        AppException negativo = catchThrowableOfType(
                () -> NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("-1"), REQUISICAO_UUID),
                AppException.class);

        assertThat(zero.getStatus()).isEqualTo(400);
        assertThat(negativo.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("cancela somente NF ATIVA e registra motivo e data")
    void cancelaNfAtiva() {
        NfEntrada nf = NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("459.00"), REQUISICAO_UUID);

        NfEntrada cancelada = nf.cancelar("Nota emitida em duplicidade");

        assertThat(nf.getStatus()).isEqualTo(StatusNfEntrada.ATIVA);
        assertThat(cancelada.getId()).isEqualTo(nf.getId());
        assertThat(cancelada.getStatus()).isEqualTo(StatusNfEntrada.CANCELADA);
        assertThat(cancelada.getMotivoCancelamento()).isEqualTo("Nota emitida em duplicidade");
        assertThat(cancelada.getDataCancelamento()).isNotNull();
    }

    @Test
    @DisplayName("rejeita cancelamento de NF ja cancelada")
    void rejeitaCancelamentoDuplicado() {
        NfEntrada cancelada = NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("459.00"), REQUISICAO_UUID)
                .cancelar("Duplicidade");

        AppException ex = catchThrowableOfType(
                () -> cancelada.cancelar("Nova tentativa"), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("rejeita motivo de cancelamento vazio")
    void rejeitaMotivoVazio() {
        NfEntrada nf = NfEntrada.criar(CHAVE_VALIDA, new BigDecimal("459.00"), REQUISICAO_UUID);

        AppException ex = catchThrowableOfType(() -> nf.cancelar("  "), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("reconstitute preserva todos os campos persisted")
    void reconstitutePreservaDados() {
        UUID id = UUID.randomUUID();
        LocalDateTime cancelamento = LocalDateTime.of(2026, 11, 27, 10, 30);
        LocalDateTime criado = LocalDateTime.of(2026, 11, 26, 10, 0);

        NfEntrada nf = NfEntrada.reconstitute(
                id, CHAVE_VALIDA, new BigDecimal("459.00"), REQUISICAO_UUID,
                StatusNfEntrada.CANCELADA, "Duplicidade", cancelamento, criado);

        assertThat(nf.getId()).isEqualTo(id);
        assertThat(nf.getStatus()).isEqualTo(StatusNfEntrada.CANCELADA);
        assertThat(nf.getMotivoCancelamento()).isEqualTo("Duplicidade");
        assertThat(nf.getDataCancelamento()).isEqualTo(cancelamento);
        assertThat(nf.getCreatedAt()).isEqualTo(criado);
    }
}
