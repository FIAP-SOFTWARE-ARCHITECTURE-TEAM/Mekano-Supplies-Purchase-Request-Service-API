package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.infrastructure.entity.ItemRequisicaoCompraEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ItemRequisicaoCompraPanacheRepository implements PanacheRepository<ItemRequisicaoCompraEntity> {

    public List<ItemRequisicaoCompraEntity> findByRequisicaoUuid(UUID requisicaoUuid) {
        return list("requisicaoUuid = ?1 order by id", requisicaoUuid);
    }

    public List<ItemRequisicaoCompraEntity> findByRequisicaoUuids(List<UUID> requisicaoUuids) {
        if (requisicaoUuids == null || requisicaoUuids.isEmpty()) {
            return List.of();
        }
        return list("requisicaoUuid in ?1 order by id", requisicaoUuids);
    }
}
