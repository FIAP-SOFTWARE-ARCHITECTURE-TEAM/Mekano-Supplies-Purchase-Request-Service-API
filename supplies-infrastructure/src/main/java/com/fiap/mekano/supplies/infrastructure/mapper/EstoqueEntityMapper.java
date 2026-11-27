package com.fiap.mekano.supplies.infrastructure.mapper;

import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.infrastructure.entity.EstoqueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface EstoqueEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "pecaUuid", source = "pecaUuid")
    @Mapping(target = "saldoAtual", source = "saldoAtual")
    @Mapping(target = "saldoReservado", source = "saldoReservado")
    @Mapping(target = "estoqueMinimo", source = "estoqueMinimo")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    EstoqueEntity toEntity(Estoque estoque);

    default Estoque toDomain(EstoqueEntity entity) {
        return Estoque.reconstitute(
                entity.getUuid(),
                entity.getPecaUuid(),
                entity.getSaldoAtual(),
                entity.getSaldoReservado(),
                entity.getEstoqueMinimo(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
