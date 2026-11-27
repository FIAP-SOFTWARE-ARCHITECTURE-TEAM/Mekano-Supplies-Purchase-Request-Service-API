package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ItemRequisicaoCompraTest {

    private static final UUID PECA = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("cria item com snapshots e normaliza valor para duas casas")
    void criaItemValido() {
        ItemRequisicaoCompra item = ItemRequisicaoCompra.criar(
                PECA, "PEA-001", "Filtro de Oleo", 10L, new BigDecimal("45.9"));

        assertThat(item.getId()).isNotNull();
        assertThat(item.getPecaUuid()).isEqualTo(PECA);
        assertThat(item.getPecaCodigo()).isEqualTo("PEA-001");
        assertThat(item.getPecaDescricao()).isEqualTo("Filtro de Oleo");
        assertThat(item.getQuantidade()).isEqualTo(10L);
        assertThat(item.getValorUnitario()).isEqualByComparingTo("45.90");
        assertThat(item.getValorUnitario().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("reconstitute preserva o UUID do item")
    void reconstitutePreservaId() {
        UUID id = UUID.randomUUID();

        ItemRequisicaoCompra item = ItemRequisicaoCompra.reconstitute(
                id, PECA, "PEA-001", "Filtro de Oleo", 1L, new BigDecimal("10.00"));

        assertThat(item.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("rejeita quantidade nula ou menor/igual a zero")
    void rejeitaQuantidadeInvalida() {
        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, "PEA-001", "Filtro", null, BigDecimal.ONE),
                AppException.class).getStatus()).isEqualTo(400);

        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, "PEA-001", "Filtro", 0L, BigDecimal.ONE),
                AppException.class).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejeita peca, codigo e descricao obrigatorios")
    void rejeitaCamposObrigatorios() {
        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(null, "PEA-001", "Filtro", 1L, BigDecimal.ONE),
                AppException.class).getStatus()).isEqualTo(400);

        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, " ", "Filtro", 1L, BigDecimal.ONE),
                AppException.class).getStatus()).isEqualTo(400);

        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, "PEA-001", null, 1L, BigDecimal.ONE),
                AppException.class).getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("rejeita valor unitario nulo ou nao positivo")
    void rejeitaValorInvalido() {
        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, "PEA-001", "Filtro", 1L, null),
                AppException.class).getStatus()).isEqualTo(400);

        assertThat(catchThrowableOfType(
                () -> ItemRequisicaoCompra.criar(PECA, "PEA-001", "Filtro", 1L, BigDecimal.ZERO),
                AppException.class).getStatus()).isEqualTo(400);
    }
}
