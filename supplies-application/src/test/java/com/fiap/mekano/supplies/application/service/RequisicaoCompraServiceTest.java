package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.supplies.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequisicaoCompraServiceTest {

    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    RequisicaoCompraRepositoryPort repository;

    @Mock
    PecaCatalogPort pecaCatalog;

    @InjectMocks
    RequisicaoCompraService service;

    @Test
    @DisplayName("cria requisicao com snapshots retornados pelo catalogo")
    void criaComSucesso() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenReturn(new PecaCatalogResult(
                        List.of(new PecaCatalogItem(PECA_A, "PEA-001", "Filtro de Oleo",
                                new BigDecimal("45.90"), true)),
                        List.of()));
        when(repository.save(any(RequisicaoCompra.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new CreateRequisicaoCompraCommand(
                List.of(new ItemRequisicaoCompraCommand(PECA_A, 5L)), "Reposicao");

        RequisicaoCompra resultado = service.criar(command);

        assertThat(resultado.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(resultado.getMotivo()).isEqualTo(MotivoRequisicao.ESTOQUE_MINIMO);
        assertThat(resultado.getObservacao()).isEqualTo("Reposicao");
        assertThat(resultado.getItens()).hasSize(1);
        assertThat(resultado.getItens().get(0).getPecaCodigo()).isEqualTo("PEA-001");
        assertThat(resultado.getItens().get(0).getPecaDescricao()).isEqualTo("Filtro de Oleo");
        assertThat(resultado.getItens().get(0).getValorUnitario()).isEqualByComparingTo("45.90");
        assertThat(resultado.getItens().get(0).getQuantidade()).isEqualTo(5L);

        verify(pecaCatalog).buscarPorUuids(argThat(uuids -> uuids.size() == 1 && uuids.contains(PECA_A)));
        verify(repository).save(any(RequisicaoCompra.class));
    }

    @Test
    @DisplayName("retorna 404 quando peca nao existe no catalogo")
    void pecaNaoEncontrada() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenReturn(new PecaCatalogResult(List.of(), List.of(PECA_A)));

        AppException ex = catchThrowableOfType(
                () -> service.criar(command(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(404);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("retorna 409 quando peca esta inativa")
    void pecaInativa() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenReturn(new PecaCatalogResult(
                        List.of(new PecaCatalogItem(PECA_A, "PEA-001", "Filtro",
                                new BigDecimal("45.90"), false)),
                        List.of()));

        AppException ex = catchThrowableOfType(
                () -> service.criar(command(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("retorna 400 para peca duplicada sem consultar o Registry")
    void pecaDuplicada() {
        var command = new CreateRequisicaoCompraCommand(
                List.of(new ItemRequisicaoCompraCommand(PECA_A, 1L),
                        new ItemRequisicaoCompraCommand(PECA_A, 2L)),
                null);

        AppException ex = catchThrowableOfType(() -> service.criar(command), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(400);
        verifyNoInteractions(pecaCatalog, repository);
    }

    @Test
    @DisplayName("propaga 503 quando o Registry esta indisponivel")
    void registryIndisponivel() {
        when(pecaCatalog.buscarPorUuids(anyCollection()))
                .thenThrow(new AppException(503, "Registry indisponivel"));

        AppException ex = catchThrowableOfType(
                () -> service.criar(command(PECA_A)),
                AppException.class);

        assertThat(ex.getStatus()).isEqualTo(503);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("retorna 404 ao buscar requisicao inexistente")
    void buscarInexistente() {
        when(repository.findById(PECA_A)).thenReturn(Optional.empty());

        AppException ex = catchThrowableOfType(() -> service.buscar(PECA_A), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("lista com paginacao e limita size a 100")
    void listaComPaginacao() {
        RequisicaoCompra requisicao = requisicaoExistente(PECA_A);
        var filtro = new RequisicaoCompraFiltro(StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, PECA_A);

        when(repository.findAll(any(RequisicaoCompraFiltro.class), eq(0), eq(100)))
                .thenReturn(List.of(requisicao));
        when(repository.count(any(RequisicaoCompraFiltro.class))).thenReturn(1L);

        var page = service.listar(filtro, 0, 500);

        assertThat(page.size()).isEqualTo(100);
        assertThat(page.page()).isZero();
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("usa size padrao 20 quando invalido")
    void usaSizePadrao() {
        when(repository.findAll(any(RequisicaoCompraFiltro.class), eq(0), eq(20)))
                .thenReturn(List.of());
        when(repository.count(any(RequisicaoCompraFiltro.class))).thenReturn(0L);

        var page = service.listar(null, -1, 0);

        assertThat(page.size()).isEqualTo(20);
        assertThat(page.page()).isZero();
        assertThat(page.content()).isEmpty();
        assertThat(page.totalPages()).isZero();
    }

    private static CreateRequisicaoCompraCommand command(UUID pecaUuid) {
        return new CreateRequisicaoCompraCommand(
                List.of(new ItemRequisicaoCompraCommand(pecaUuid, 1L)), null);
    }

    private static RequisicaoCompra requisicaoExistente(UUID pecaUuid) {
        var item = com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra.criar(
                pecaUuid, "PEA-001", "Filtro", 1L, new BigDecimal("10.00"));
        return RequisicaoCompra.criar(List.of(item), null);
    }
}
