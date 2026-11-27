package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class EstoqueTest {

    private static final UUID PECA_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    @DisplayName("aplica credito, debito, reserva, liberacao e consumo")
    void aplicaMovimentos() {
        Estoque estoque = Estoque.criar(PECA_UUID)
                .aplicar(TipoMovimentoEstoque.CREDITO, 10)
                .aplicar(TipoMovimentoEstoque.RESERVA, 4)
                .aplicar(TipoMovimentoEstoque.DEBITO, 2)
                .aplicar(TipoMovimentoEstoque.LIBERACAO, 1)
                .aplicar(TipoMovimentoEstoque.CONSUMO, 2);

        assertThat(estoque.getSaldoAtual()).isEqualTo(6);
        assertThat(estoque.getSaldoReservado()).isEqualTo(1);
        assertThat(estoque.disponivel()).isEqualTo(5);
        assertThat(estoque.getVersion()).isEqualTo(5);
    }

    @Test
    @DisplayName("nao permite debito, liberacao ou consumo acima dos saldos")
    void rejeitaSaldoInsuficiente() {
        Estoque estoque = Estoque.criar(PECA_UUID);

        assertThat(catchThrowableOfType(
                () -> estoque.aplicar(TipoMovimentoEstoque.DEBITO, 1), AppException.class).getStatus())
                .isEqualTo(409);
        assertThat(catchThrowableOfType(
                () -> estoque.aplicar(TipoMovimentoEstoque.LIBERACAO, 1), AppException.class).getStatus())
                .isEqualTo(409);
        assertThat(catchThrowableOfType(
                () -> estoque.aplicar(TipoMovimentoEstoque.CONSUMO, 1), AppException.class).getStatus())
                .isEqualTo(409);
        assertThat(estoque.getSaldoAtual()).isZero();
        assertThat(estoque.getSaldoReservado()).isZero();
    }

    @Test
    @DisplayName("permite saldo reservado maior que o saldo atual")
    void saldoReservadoPodeExcederAtual() {
        Estoque estoque = Estoque.criar(PECA_UUID)
                .aplicar(TipoMovimentoEstoque.RESERVA, 3);

        assertThat(estoque.getSaldoAtual()).isZero();
        assertThat(estoque.getSaldoReservado()).isEqualTo(3);
        assertThat(estoque.disponivel()).isEqualTo(-3);
    }

    @Test
    @DisplayName("identifica estoque disponivel abaixo ou igual ao minimo")
    void identificaAbaixoDoMinimo() {
        Estoque estoque = Estoque.reconstitute(
                UUID.randomUUID(), PECA_UUID, 5, 2, 3L, 0, null, null);
        Estoque semMinimo = Estoque.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), 0, 0, null, 0, null, null);

        assertThat(estoque.abaixoDoMinimo()).isTrue();
        assertThat(semMinimo.abaixoDoMinimo()).isFalse();
    }

    @Test
    @DisplayName("rejeita quantidade zero e tipo ausente")
    void validaMovimento() {
        Estoque estoque = Estoque.criar(PECA_UUID);

        assertThat(catchThrowableOfType(
                () -> estoque.aplicar(TipoMovimentoEstoque.CREDITO, 0), AppException.class).getStatus())
                .isEqualTo(400);
        assertThat(catchThrowableOfType(
                () -> estoque.aplicar(null, 1), AppException.class).getStatus())
                .isEqualTo(400);
    }
}
