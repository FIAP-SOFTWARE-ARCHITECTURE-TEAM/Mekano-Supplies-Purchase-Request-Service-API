package com.fiap.mekano.supplies.infrastructure.mapper;

import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.StatusNfEntrada;
import com.fiap.mekano.supplies.infrastructure.entity.NfEntradaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface NfEntradaEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", source = "id")
    @Mapping(target = "requisicaoUuid", source = "requisicaoUuid")
    @Mapping(target = "chaveAcesso", source = "chaveAcesso")
    @Mapping(target = "valorTotal", source = "valorTotal")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "motivoCancelamento", source = "motivoCancelamento")
    @Mapping(target = "dataCancelamento", source = "dataCancelamento")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    NfEntradaEntity toEntity(NfEntrada nfEntrada);

    default NfEntrada toDomain(NfEntradaEntity entity) {
        return NfEntrada.reconstitute(
                entity.getUuid(),
                entity.getChaveAcesso(),
                entity.getValorTotal(),
                entity.getRequisicaoUuid(),
                StatusNfEntrada.valueOf(entity.getStatus()),
                entity.getMotivoCancelamento(),
                entity.getDataCancelamento(),
                entity.getCreatedAt());
    }
}
