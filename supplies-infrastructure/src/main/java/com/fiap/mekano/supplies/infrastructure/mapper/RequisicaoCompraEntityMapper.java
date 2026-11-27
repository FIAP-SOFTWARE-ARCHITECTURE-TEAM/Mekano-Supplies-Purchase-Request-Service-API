package com.fiap.mekano.supplies.infrastructure.mapper;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.infrastructure.entity.RequisicaoCompraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "motivo", source = "motivo")
    @Mapping(target = "observacao", source = "observacao")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    RequisicaoCompraEntity toEntity(RequisicaoCompra requisicao);

    default RequisicaoCompra toDomain(RequisicaoCompraEntity entity, List<ItemRequisicaoCompra> itens) {
        return RequisicaoCompra.reconstitute(
                entity.getUuid(),
                itens,
                StatusRequisicao.valueOf(entity.getStatus()),
                MotivoRequisicao.valueOf(entity.getMotivo()),
                entity.getObservacao(),
                entity.getCreatedAt());
    }
}
