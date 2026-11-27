package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.NfEntradaDetalhe;
import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import com.fiap.mekano.supplies.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.supplies.domain.port.in.EstoqueServicePort;
import com.fiap.mekano.supplies.domain.port.in.MovimentarEstoqueCommand;
import com.fiap.mekano.supplies.domain.port.in.NfEntradaServicePort;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaService implements NfEntradaServicePort {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NfEntradaRepositoryPort nfRepository;
    private final RequisicaoCompraRepositoryPort requisicaoRepository;
    private final EstoqueServicePort estoqueService;
    private final MovimentoEstoqueRepositoryPort movimentoRepository;

    public NfEntradaService(NfEntradaRepositoryPort nfRepository,
                            RequisicaoCompraRepositoryPort requisicaoRepository,
                            EstoqueServicePort estoqueService,
                            MovimentoEstoqueRepositoryPort movimentoRepository) {
        this.nfRepository = nfRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.estoqueService = estoqueService;
        this.movimentoRepository = movimentoRepository;
    }

    @Override
    @Transactional
    public NfEntradaDetalhe registrar(CreateNfEntradaCommand command) {
        if (command == null) {
            throw new AppException(400, Messages.get("nf_entrada.dados.required"));
        }

        NfEntrada nf = NfEntrada.criar(command.chaveAcesso(), command.valorTotal(), command.requisicaoUuid());
        RequisicaoCompra requisicao = buscarRequisicao(nf.getRequisicaoUuid());
        validarStatusRequisicao(requisicao);

        if (nfRepository.findByChaveAcesso(nf.getChaveAcesso()).isPresent()) {
            Log.warnf("Chave de acesso duplicada: chaveAcesso=%s", nf.getChaveAcesso());
            throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada"));
        }

        NfEntrada salva = nfRepository.save(nf);
        for (ItemRequisicaoCompra item : requisicao.getItens()) {
            movimentar(item, nf.getId(), TipoMovimentoEstoque.CREDITO,
                    OrigemMovimentoEstoque.NF_ENTRADA, "CREDITO");
        }

        Log.infof("NF de entrada registrada: nfUuid=%s, requisicaoUuid=%s, itens=%d",
                salva.getId(), salva.getRequisicaoUuid(), requisicao.getItens().size());
        return detalhar(salva, requisicao);
    }

    @Override
    public PageResult<NfEntrada> listar(NfEntradaFiltro filtro, int page, int size) {
        int paginaNormalizada = Math.max(page, 0);
        int tamanhoNormalizado = normalizarTamanho(size);
        NfEntradaFiltro filtroNormalizado = filtro == null ? NfEntradaFiltro.vazio() : filtro;

        List<NfEntrada> content = nfRepository.findAll(filtroNormalizado, paginaNormalizada, tamanhoNormalizado);
        long total = nfRepository.count(filtroNormalizado);
        return PageResult.of(content, paginaNormalizada, tamanhoNormalizado, total);
    }

    @Override
    public NfEntradaDetalhe buscar(UUID id) {
        NfEntrada nf = buscarNf(id);
        RequisicaoCompra requisicao = buscarRequisicao(nf.getRequisicaoUuid());
        return detalhar(nf, requisicao);
    }

    @Override
    @Transactional
    public NfEntradaDetalhe cancelar(UUID id, String motivo) {
        NfEntrada nf = buscarNf(id);
        RequisicaoCompra requisicao = buscarRequisicao(nf.getRequisicaoUuid());
        NfEntrada cancelada = nf.cancelar(motivo);

        for (ItemRequisicaoCompra item : requisicao.getItens()) {
            movimentar(item, nf.getId(), TipoMovimentoEstoque.DEBITO,
                    OrigemMovimentoEstoque.NF_ENTRADA_CANCELAMENTO, "ESTORNO");
        }

        NfEntrada salva = nfRepository.save(cancelada);
        Log.infof("NF de entrada cancelada com estorno: nfUuid=%s, itens=%d",
                salva.getId(), requisicao.getItens().size());
        return detalhar(salva, requisicao);
    }

    private NfEntrada buscarNf(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("nf_entrada.id.required"));
        }
        return nfRepository.findById(id)
                .orElseThrow(() -> {
                    Log.warnf("NF de entrada nao encontrada: nfUuid=%s", id);
                    return new AppException(404, Messages.get("nf_entrada.not.found", id));
                });
    }

    private RequisicaoCompra buscarRequisicao(UUID id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> {
                    Log.warnf("Requisicao de compra nao encontrada: requisicaoUuid=%s", id);
                    return new AppException(404, Messages.get("requisicao_compra.not.found", id));
                });
    }

    private static void validarStatusRequisicao(RequisicaoCompra requisicao) {
        if (requisicao.getStatus() != StatusRequisicao.PRODUTO_RECEBIDO) {
            Log.warnf("Status invalido para NF: requisicaoUuid=%s, status=%s",
                    requisicao.getId(), requisicao.getStatus());
            throw new AppException(409, Messages.get("nf_entrada.status.requisicao.invalido",
                    StatusRequisicao.PRODUTO_RECEBIDO.name()));
        }
    }

    private void movimentar(ItemRequisicaoCompra item, UUID nfUuid, TipoMovimentoEstoque tipo,
                            OrigemMovimentoEstoque origem, String sufixo) {
        String idempotencyKey = "nf:%s:%s:%s".formatted(nfUuid, item.getPecaUuid(), sufixo);
        estoqueService.movimentar(new MovimentarEstoqueCommand(
                item.getPecaUuid(),
                tipo,
                item.getQuantidade(),
                origem,
                nfUuid,
                idempotencyKey));
    }

    private NfEntradaDetalhe detalhar(NfEntrada nf, RequisicaoCompra requisicao) {
        return new NfEntradaDetalhe(
                nf,
                requisicao.getItens(),
                movimentoRepository.findByReferenciaUuid(nf.getId()));
    }

    private static int normalizarTamanho(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
