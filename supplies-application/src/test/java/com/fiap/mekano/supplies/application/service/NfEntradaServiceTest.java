package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusNfEntrada;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import com.fiap.mekano.supplies.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.supplies.domain.port.in.EstoqueServicePort;
import com.fiap.mekano.supplies.domain.port.in.MovimentarEstoqueCommand;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfEntradaServiceTest {

    private static final UUID REQUISICAO_UUID = UUID.randomUUID();
    private static final UUID PECA_A = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PECA_B = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String CHAVE = "35200612345678000190550000001234567890123456";

    @Mock
    NfEntradaRepositoryPort nfRepository;

    @Mock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @Mock
    EstoqueServicePort estoqueService;

    @Mock
    MovimentoEstoqueRepositoryPort movimentoRepository;

    @InjectMocks
    NfEntradaService service;

    @Test
    @DisplayName("registra NF e credita estoque com chaves de idempotencia por item")
    void registrarCreditaEstoque() {
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));
        when(nfRepository.findByChaveAcesso(CHAVE)).thenReturn(Optional.empty());
        when(nfRepository.save(any(NfEntrada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimentoRepository.findByReferenciaUuid(any())).thenReturn(List.of());

        var resultado = service.registrar(command(CHAVE, "459.00"));

        assertThat(resultado.nfEntrada().getStatus()).isEqualTo(StatusNfEntrada.ATIVA);
        assertThat(resultado.itens()).hasSize(2);

        ArgumentCaptor<MovimentarEstoqueCommand> captor = ArgumentCaptor.forClass(MovimentarEstoqueCommand.class);
        verify(estoqueService, times(2)).movimentar(captor.capture());
        List<MovimentarEstoqueCommand> movimentos = captor.getAllValues();

        assertThat(movimentos).extracting(MovimentarEstoqueCommand::tipo)
                .containsExactly(TipoMovimentoEstoque.CREDITO, TipoMovimentoEstoque.CREDITO);
        assertThat(movimentos).extracting(MovimentarEstoqueCommand::origem)
                .containsExactly(OrigemMovimentoEstoque.NF_ENTRADA, OrigemMovimentoEstoque.NF_ENTRADA);
        assertThat(movimentos).extracting(MovimentarEstoqueCommand::idempotencyKey)
                .containsExactly(
                        "nf:%s:%s:CREDITO".formatted(resultado.nfEntrada().getId(), PECA_A),
                        "nf:%s:%s:CREDITO".formatted(resultado.nfEntrada().getId(), PECA_B));
        verify(nfRepository).save(any(NfEntrada.class));
    }

    @Test
    @DisplayName("retorna 409 quando a chave de acesso ja existe")
    void registraChaveDuplicada() {
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));
        when(nfRepository.findByChaveAcesso(CHAVE)).thenReturn(Optional.of(nfAtiva()));

        AppException ex = catchThrowableOfType(
                () -> service.registrar(command(CHAVE, "459.00")), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verify(nfRepository, never()).save(any());
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("retorna 409 quando a requisicao nao esta PRODUTO_RECEBIDO")
    void registraComRequisicaoInvalida() {
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoAberta()));

        AppException ex = catchThrowableOfType(
                () -> service.registrar(command(CHAVE, "459.00")), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verify(nfRepository, never()).save(any());
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("retorna 400 para chave invalida e valor menor ou igual a zero")
    void registraDadosInvalidos() {
        AppException chave = catchThrowableOfType(
                () -> service.registrar(command("123", "459.00")), AppException.class);
        AppException valor = catchThrowableOfType(
                () -> service.registrar(command(CHAVE, "0")), AppException.class);

        assertThat(chave.getStatus()).isEqualTo(400);
        assertThat(valor.getStatus()).isEqualTo(400);
        verifyNoInteractions(nfRepository, requisicaoRepository, estoqueService);
    }

    @Test
    @DisplayName("retorna 404 quando a requisicao vinculada nao existe")
    void registraRequisicaoInexistente() {
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.empty());

        AppException ex = catchThrowableOfType(
                () -> service.registrar(command(CHAVE, "459.00")), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(404);
        verify(nfRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancela NF ATIVA e grava movimentos de estorno")
    void cancelaComEstorno() {
        NfEntrada nf = nfAtiva();
        when(nfRepository.findById(nf.getId())).thenReturn(Optional.of(nf));
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));
        when(nfRepository.save(any(NfEntrada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimentoRepository.findByReferenciaUuid(nf.getId())).thenReturn(List.of(movimentoEstorno(nf)));

        var resultado = service.cancelar(nf.getId(), "Nota emitida em duplicidade");

        assertThat(resultado.nfEntrada().getStatus()).isEqualTo(StatusNfEntrada.CANCELADA);
        assertThat(resultado.movimentos()).hasSize(1);

        ArgumentCaptor<MovimentarEstoqueCommand> captor = ArgumentCaptor.forClass(MovimentarEstoqueCommand.class);
        verify(estoqueService, times(2)).movimentar(captor.capture());
        assertThat(captor.getAllValues()).extracting(MovimentarEstoqueCommand::tipo)
                .containsExactly(TipoMovimentoEstoque.DEBITO, TipoMovimentoEstoque.DEBITO);
        assertThat(captor.getAllValues()).extracting(MovimentarEstoqueCommand::origem)
                .containsExactly(OrigemMovimentoEstoque.NF_ENTRADA_CANCELAMENTO,
                        OrigemMovimentoEstoque.NF_ENTRADA_CANCELAMENTO);
        assertThat(captor.getAllValues()).extracting(MovimentarEstoqueCommand::idempotencyKey)
                .containsExactly(
                        "nf:%s:%s:ESTORNO".formatted(nf.getId(), PECA_A),
                        "nf:%s:%s:ESTORNO".formatted(nf.getId(), PECA_B));
        verify(nfRepository).save(any(NfEntrada.class));
    }

    @Test
    @DisplayName("propaga 409 de saldo insuficiente sem persistir cancelamento (rollback)")
    void cancelaComSaldoInsuficiente() {
        NfEntrada nf = nfAtiva();
        when(nfRepository.findById(nf.getId())).thenReturn(Optional.of(nf));
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));
        when(estoqueService.movimentar(any(MovimentarEstoqueCommand.class)))
                .thenThrow(new AppException(409, "Saldo insuficiente"));

        AppException ex = catchThrowableOfType(
                () -> service.cancelar(nf.getId(), "Estorno"), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verify(nfRepository, never()).save(any());
        verify(movimentoRepository, never()).findByReferenciaUuid(any());
    }

    @Test
    @DisplayName("retorna 409 ao tentar cancelar NF ja cancelada")
    void cancelaNfJaCancelada() {
        NfEntrada cancelada = nfAtiva().cancelar("Duplicidade");
        when(nfRepository.findById(cancelada.getId())).thenReturn(Optional.of(cancelada));
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));

        AppException ex = catchThrowableOfType(
                () -> service.cancelar(cancelada.getId(), "Outro motivo"), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(409);
        verifyNoInteractions(estoqueService);
        verify(nfRepository, never()).save(any());
    }

    @Test
    @DisplayName("lista com filtro e normaliza paginacao")
    void listaComFiltro() {
        NfEntradaFiltro filtro = new NfEntradaFiltro(StatusNfEntrada.ATIVA, REQUISICAO_UUID, null);
        when(nfRepository.findAll(filtro, 0, 100)).thenReturn(List.of(nfAtiva()));
        when(nfRepository.count(filtro)).thenReturn(1L);

        var page = service.listar(filtro, -1, 500);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
    }

    @Test
    @DisplayName("retorna 404 ao buscar NF inexistente")
    void buscarInexistente() {
        UUID id = UUID.randomUUID();
        when(nfRepository.findById(id)).thenReturn(Optional.empty());

        AppException ex = catchThrowableOfType(() -> service.buscar(id), AppException.class);

        assertThat(ex.getStatus()).isEqualTo(404);
        verify(requisicaoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("busca detalhe com itens e movimentos da NF")
    void buscarComDetalhe() {
        NfEntrada nf = nfAtiva();
        when(nfRepository.findById(nf.getId())).thenReturn(Optional.of(nf));
        when(requisicaoRepository.findById(REQUISICAO_UUID)).thenReturn(Optional.of(requisicaoRecebida()));
        when(movimentoRepository.findByReferenciaUuid(nf.getId())).thenReturn(List.of(movimentoEstorno(nf)));

        var resultado = service.buscar(nf.getId());

        assertThat(resultado.nfEntrada().getId()).isEqualTo(nf.getId());
        assertThat(resultado.itens()).hasSize(2);
        assertThat(resultado.movimentos()).hasSize(1);
    }

    private static CreateNfEntradaCommand command(String chave, String valor) {
        return new CreateNfEntradaCommand(REQUISICAO_UUID, chave, new BigDecimal(valor));
    }

    private static NfEntrada nfAtiva() {
        return NfEntrada.criar(CHAVE, new BigDecimal("459.00"), REQUISICAO_UUID);
    }

    private static RequisicaoCompra requisicaoRecebida() {
        return RequisicaoCompra.reconstitute(
                REQUISICAO_UUID, itens(), StatusRequisicao.PRODUTO_RECEBIDO,
                MotivoRequisicao.ESTOQUE_MINIMO, null, LocalDateTime.now());
    }

    private static RequisicaoCompra requisicaoAberta() {
        return RequisicaoCompra.reconstitute(
                REQUISICAO_UUID, itens(), StatusRequisicao.ABERTA,
                MotivoRequisicao.ESTOQUE_MINIMO, null, LocalDateTime.now());
    }

    private static List<ItemRequisicaoCompra> itens() {
        return List.of(
                ItemRequisicaoCompra.criar(PECA_A, "PEA-001", "Filtro de Oleo", 10L, new BigDecimal("45.90")),
                ItemRequisicaoCompra.criar(PECA_B, "PEA-002", "Correia", 5L, new BigDecimal("32.50")));
    }

    private static MovimentoEstoque movimentoEstorno(NfEntrada nf) {
        return MovimentoEstoque.criar(
                PECA_A, TipoMovimentoEstoque.DEBITO, 10L, 0L, 0L,
                OrigemMovimentoEstoque.NF_ENTRADA_CANCELAMENTO, nf.getId(),
                "nf:%s:%s:ESTORNO".formatted(nf.getId(), PECA_A));
    }
}
