package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraRepositoryPort;
import com.fiap.mekano.supplies.infrastructure.entity.ItemRequisicaoCompraEntity;
import com.fiap.mekano.supplies.infrastructure.entity.RequisicaoCompraEntity;
import com.fiap.mekano.supplies.infrastructure.mapper.ItemRequisicaoCompraEntityMapper;
import com.fiap.mekano.supplies.infrastructure.mapper.RequisicaoCompraEntityMapper;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class RequisicaoCompraRepositoryImpl implements RequisicaoCompraRepositoryPort {

    private final RequisicaoCompraPanacheRepository panache;
    private final ItemRequisicaoCompraPanacheRepository itemPanache;
    private final RequisicaoCompraEntityMapper mapper;
    private final ItemRequisicaoCompraEntityMapper itemMapper;

    public RequisicaoCompraRepositoryImpl(RequisicaoCompraPanacheRepository panache,
                                          ItemRequisicaoCompraPanacheRepository itemPanache,
                                          RequisicaoCompraEntityMapper mapper,
                                          ItemRequisicaoCompraEntityMapper itemMapper) {
        this.panache = panache;
        this.itemPanache = itemPanache;
        this.mapper = mapper;
        this.itemMapper = itemMapper;
    }

    @Override
    public RequisicaoCompra save(RequisicaoCompra requisicao) {
        RequisicaoCompraEntity entity = panache.find("uuid", requisicao.getId()).firstResult();
        if (entity == null) {
            entity = mapper.toEntity(requisicao);
            panache.persist(entity);
        } else {
            entity.setStatus(requisicao.getStatus().name());
            entity.setMotivo(requisicao.getMotivo().name());
            entity.setObservacao(requisicao.getObservacao());
            entity.setUpdatedAt(LocalDateTime.now());
        }

        sincronizarItens(requisicao);
        return toDomainList(List.of(entity)).get(0);
    }

    @Override
    public Optional<RequisicaoCompra> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return panache.find("uuid", id).firstResultOptional()
                .map(entity -> toDomainList(List.of(entity)).get(0));
    }

    @Override
    public List<RequisicaoCompra> findAll(RequisicaoCompraFiltro filtro, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String query = buildQuery(filtro, params);
        List<RequisicaoCompraEntity> entities = panache.find(query + " order by r.id desc", params)
                .page(Page.of(page, size))
                .list();
        return toDomainList(entities);
    }

    @Override
    public long count(RequisicaoCompraFiltro filtro) {
        Map<String, Object> params = new HashMap<>();
        String query = buildQuery(filtro, params);
        return panache.count(query, params);
    }

    private void sincronizarItens(RequisicaoCompra requisicao) {
        List<ItemRequisicaoCompraEntity> existentes = itemPanache.findByRequisicaoUuid(requisicao.getId());
        Map<UUID, ItemRequisicaoCompraEntity> existentesPorUuid = existentes.stream()
                .collect(Collectors.toMap(ItemRequisicaoCompraEntity::getUuid, item -> item));

        Set<UUID> uuidsDesejados = new HashSet<>();
        for (ItemRequisicaoCompra item : requisicao.getItens()) {
            uuidsDesejados.add(item.getId());
            ItemRequisicaoCompraEntity existente = existentesPorUuid.get(item.getId());
            if (existente == null) {
                ItemRequisicaoCompraEntity novo = itemMapper.toEntity(item);
                novo.setRequisicaoUuid(requisicao.getId());
                itemPanache.persist(novo);
            } else {
                existente.setPecaUuid(item.getPecaUuid());
                existente.setPecaCodigo(item.getPecaCodigo());
                existente.setPecaDescricao(item.getPecaDescricao());
                existente.setQuantidade(item.getQuantidade());
                existente.setValorUnitario(item.getValorUnitario());
            }
        }

        for (ItemRequisicaoCompraEntity existente : existentes) {
            if (!uuidsDesejados.contains(existente.getUuid())) {
                itemPanache.delete(existente);
            }
        }
    }

    private List<RequisicaoCompra> toDomainList(List<RequisicaoCompraEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<UUID> uuids = entities.stream().map(RequisicaoCompraEntity::getUuid).toList();
        Map<UUID, List<ItemRequisicaoCompra>> itensPorRequisicao = itemPanache.findByRequisicaoUuids(uuids).stream()
                .collect(Collectors.groupingBy(
                        ItemRequisicaoCompraEntity::getRequisicaoUuid,
                        Collectors.mapping(itemMapper::toDomain, Collectors.toList())));

        return entities.stream()
                .map(entity -> mapper.toDomain(entity,
                        itensPorRequisicao.getOrDefault(entity.getUuid(), List.of())))
                .toList();
    }

    private static String buildQuery(RequisicaoCompraFiltro filtro, Map<String, Object> params) {
        StringBuilder query = new StringBuilder("from RequisicaoCompraEntity r where 1 = 1");

        if (filtro != null && filtro.status() != null) {
            query.append(" and r.status = :status");
            params.put("status", filtro.status().name());
        }
        if (filtro != null && filtro.motivo() != null) {
            query.append(" and r.motivo = :motivo");
            params.put("motivo", filtro.motivo().name());
        }
        if (filtro != null && filtro.pecaUuid() != null) {
            query.append(" and exists (select 1 from ItemRequisicaoCompraEntity i")
                    .append(" where i.requisicaoUuid = r.uuid and i.pecaUuid = :pecaUuid)");
            params.put("pecaUuid", filtro.pecaUuid());
        }

        return query.toString();
    }
}
