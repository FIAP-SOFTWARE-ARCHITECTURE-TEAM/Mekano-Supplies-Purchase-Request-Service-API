package com.fiap.mekano.supplies.infrastructure.mapper;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.infrastructure.entity.ItemRequisicaoCompraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface ItemRequisicaoCompraEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "requisicaoUuid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    ItemRequisicaoCompraEntity toEntity(ItemRequisicaoCompra item);

    default ItemRequisicaoCompra toDomain(ItemRequisicaoCompraEntity entity) {
        return ItemRequisicaoCompra.reconstitute(
                entity.getUuid(),
                entity.getPecaUuid(),
                entity.getPecaCodigo(),
                entity.getPecaDescricao(),
                entity.getQuantidade(),
                entity.getValorUnitario());
    }
}
