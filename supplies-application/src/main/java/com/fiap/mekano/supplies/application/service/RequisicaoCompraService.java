package com.fiap.mekano.supplies.application.service;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.supplies.domain.port.in.EstoqueServicePort;
import com.fiap.mekano.supplies.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.supplies.domain.port.in.RequisicaoCompraServicePort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraService implements RequisicaoCompraServicePort {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final RequisicaoCompraRepositoryPort repository;
    private final PecaCatalogPort pecaCatalog;
    private final EstoqueServicePort estoqueService;

    public RequisicaoCompraService(RequisicaoCompraRepositoryPort repository, PecaCatalogPort pecaCatalog,
                                  EstoqueServicePort estoqueService) {
        this.repository = repository;
        this.pecaCatalog = pecaCatalog;
        this.estoqueService = estoqueService;
    }

    @Override
    @Transactional
    public RequisicaoCompra criar(CreateRequisicaoCompraCommand command) {
        List<ItemRequisicaoCompraCommand> itensCommand = validarCommand(command);
        Log.infof("Criando requisicao de compra: itens=%d", itensCommand.size());

        Set<UUID> pecaUuids = new LinkedHashSet<>();
        for (ItemRequisicaoCompraCommand item : itensCommand) {
            pecaUuids.add(item.pecaUuid());
        }

        PecaCatalogResult catalogo = pecaCatalog.buscarPorUuids(pecaUuids);
        if (catalogo == null) {
            Log.warn("Catalogo do Registry retornou resposta nula");
            throw new AppException(503, Messages.get("registry.indisponivel"));
        }
        if (!catalogo.naoEncontradas().isEmpty()) {
            UUID primeira = catalogo.naoEncontradas().get(0);
            Log.warnf("Validacao falhou: pecaUuid=%s nao encontrada no catalogo do Registry", primeira);
            throw new AppException(404, Messages.get("requisicao_compra.peca.not.found", primeira));
        }

        Map<UUID, PecaCatalogItem> pecasPorUuid = new LinkedHashMap<>();
        for (PecaCatalogItem peca : catalogo.pecas()) {
            pecasPorUuid.put(peca.uuid(), peca);
        }

        List<ItemRequisicaoCompra> itens = new ArrayList<>();
        for (ItemRequisicaoCompraCommand item : itensCommand) {
            PecaCatalogItem peca = pecasPorUuid.get(item.pecaUuid());
            if (peca == null) {
                Log.warnf("Validacao falhou: pecaUuid=%s ausente na resposta do catalogo", item.pecaUuid());
                throw new AppException(404, Messages.get("requisicao_compra.peca.not.found", item.pecaUuid()));
            }
            if (!peca.ativo()) {
                Log.warnf("Validacao falhou: pecaUuid=%s esta inativa no catalogo", item.pecaUuid());
                throw new AppException(409, Messages.get("requisicao_compra.peca.inativa", peca.codigo()));
            }
            estoqueService.obterOuCriar(peca.uuid());
            itens.add(ItemRequisicaoCompra.criar(
                    peca.uuid(),
                    peca.codigo(),
                    peca.descricao(),
                    item.quantidade(),
                    peca.valorUnitario()));
        }

        RequisicaoCompra requisicao = RequisicaoCompra.criar(itens, command.observacao());
        RequisicaoCompra salva = repository.save(requisicao);

        Log.infof("Requisicao de compra criada: requisicaoUuid=%s, itens=%d, status=%s, motivo=%s",
                salva.getId(), salva.getItens().size(), salva.getStatus(), salva.getMotivo());
        return salva;
    }

    @Override
    @Transactional
    public RequisicaoCompra enviar(UUID id) {
        RequisicaoCompra requisicao = buscar(id).enviar();
        RequisicaoCompra salva = repository.save(requisicao);
        Log.infof("Requisicao enviada: requisicaoUuid=%s", salva.getId());
        return salva;
    }

    @Override
    @Transactional
    public RequisicaoCompra receber(UUID id) {
        RequisicaoCompra requisicao = buscar(id).receber();
        RequisicaoCompra salva = repository.save(requisicao);
        Log.infof("Requisicao recebida: requisicaoUuid=%s", salva.getId());
        return salva;
    }

    @Override
    @Transactional
    public RequisicaoCompra cancelar(UUID id) {
        RequisicaoCompra requisicao = buscar(id).cancelar();
        RequisicaoCompra salva = repository.save(requisicao);
        Log.infof("Requisicao cancelada: requisicaoUuid=%s", salva.getId());
        return salva;
    }

    @Override
    public RequisicaoCompra buscar(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("requisicao_compra.id.required"));
        }
        return repository.findById(id)
                .orElseThrow(() -> {
                    Log.warnf("Requisicao de compra nao encontrada: requisicaoUuid=%s", id);
                    return new AppException(404, Messages.get("requisicao_compra.not.found", id));
                });
    }

    @Override
    public PageResult<RequisicaoCompra> listar(RequisicaoCompraFiltro filtro, int page, int size) {
        int paginaNormalizada = Math.max(page, 0);
        int tamanhoNormalizado = normalizarTamanho(size);
        RequisicaoCompraFiltro filtroNormalizado = filtro == null ? RequisicaoCompraFiltro.vazio() : filtro;

        List<RequisicaoCompra> content = repository.findAll(filtroNormalizado, paginaNormalizada, tamanhoNormalizado);
        long total = repository.count(filtroNormalizado);

        return PageResult.of(content, paginaNormalizada, tamanhoNormalizado, total);
    }

    private static List<ItemRequisicaoCompraCommand> validarCommand(CreateRequisicaoCompraCommand command) {
        if (command == null || command.itens() == null || command.itens().isEmpty()) {
            throw new AppException(400, Messages.get("requisicao_compra.itens.required"));
        }

        Set<UUID> pecasVistas = new HashSet<>();
        for (ItemRequisicaoCompraCommand item : command.itens()) {
            if (item == null || item.pecaUuid() == null) {
                throw new AppException(400, Messages.get("requisicao_compra.peca.required"));
            }
            if (item.quantidade() == null || item.quantidade() <= 0) {
                throw new AppException(400, Messages.get("requisicao_compra.quantidade.invalida"));
            }
            if (!pecasVistas.add(item.pecaUuid())) {
                throw new AppException(400, Messages.get("requisicao_compra.itens.duplicados"));
            }
        }
        return command.itens();
    }

    private static int normalizarTamanho(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
