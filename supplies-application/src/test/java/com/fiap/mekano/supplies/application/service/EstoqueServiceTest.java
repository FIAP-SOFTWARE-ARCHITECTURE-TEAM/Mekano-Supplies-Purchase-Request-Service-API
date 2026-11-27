package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import com.fiap.mekano.supplies.domain.port.in.MovimentarEstoqueCommand;
import com.fiap.mekano.supplies.domain.port.out.EstoqueRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    private static final UUID PECA_UUID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String KEY = "ajuste-001";

    @Mock
    EstoqueRepositoryPort estoqueRepository;

    @Mock
    MovimentoEstoqueRepositoryPort movimentoRepository;

    @InjectMocks
    EstoqueService service;

    @Test
    @DisplayName("registra movimento e atualiza saldo dentro da operacao")
    void movimentaEstoque() {
        when(movimentoRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(estoqueRepository.findOrCreateAndLock(PECA_UUID)).thenReturn(Estoque.criar(PECA_UUID));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(call -> call.getArgument(0));
        when(movimentoRepository.save(any(MovimentoEstoque.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.movimentar(command(10L));

        assertThat(result.idempotente()).isFalse();
        assertThat(result.movimento().getSaldoAtualApos()).isEqualTo(10);
        assertThat(result.movimento().getQuantidade()).isEqualTo(10);
        verify(estoqueRepository).save(any(Estoque.class));
        verify(movimentoRepository).save(any(MovimentoEstoque.class));
    }

    @Test
    @DisplayName("retorna movimento existente sem alterar saldo ao repetir idempotency key")
    void repeticaoIdempotente() {
        MovimentoEstoque existente = MovimentoEstoque.criar(
                PECA_UUID, TipoMovimentoEstoque.CREDITO, 10, 10, 0,
                OrigemMovimentoEstoque.AJUSTE, null, KEY);
        when(movimentoRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existente));

        var result = service.movimentar(command(10L));

        assertThat(result.idempotente()).isTrue();
        assertThat(result.movimento()).isEqualTo(existente);
        verify(estoqueRepository, never()).findOrCreateAndLock(any());
        verify(estoqueRepository, never()).save(any());
    }

    @Test
    @DisplayName("conflita quando a mesma chave e reutilizada para outra operacao")
    void conflitoDeChave() {
        MovimentoEstoque existente = MovimentoEstoque.criar(
                PECA_UUID, TipoMovimentoEstoque.CREDITO, 10, 10, 0,
                OrigemMovimentoEstoque.AJUSTE, null, KEY);
        when(movimentoRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existente));

        AppException ex = catchThrowableOfType(
                () -> service.movimentar(new MovimentarEstoqueCommand(
                        PECA_UUID, TipoMovimentoEstoque.CREDITO, 5L,
                        OrigemMovimentoEstoque.AJUSTE, null, KEY)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verify(estoqueRepository, never()).findOrCreateAndLock(any());
    }

    @Test
    @DisplayName("valida campos obrigatorios antes de consultar repositorios")
    void comandoInvalido() {
        AppException ex = catchThrowableOfType(
                () -> service.movimentar(null),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
        verify(movimentoRepository, never()).findByIdempotencyKey(any());
    }

    @Test
    @DisplayName("normaliza paginacao ao listar abaixo do minimo")
    void listaAbaixoMinimoComPaginacaoNormalizada() {
        when(estoqueRepository.findAbaixoMinimo(0, 100)).thenReturn(List.of());
        when(estoqueRepository.countAbaixoMinimo()).thenReturn(0L);

        var page = service.listarAbaixoMinimo(-1, 200);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.totalElements()).isZero();
        verify(estoqueRepository).findAbaixoMinimo(eq(0), eq(100));
    }

    private static MovimentarEstoqueCommand command(Long quantity) {
        return new MovimentarEstoqueCommand(
                PECA_UUID, TipoMovimentoEstoque.CREDITO, quantity,
                OrigemMovimentoEstoque.AJUSTE, null, KEY);
    }
}
