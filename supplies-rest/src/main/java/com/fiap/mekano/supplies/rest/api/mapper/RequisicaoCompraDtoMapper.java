package com.fiap.mekano.supplies.rest.api.mapper;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.supplies.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.supplies.rest.api.dto.ItemRequisicaoCompraResponse;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraPageResponse;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface RequisicaoCompraDtoMapper {

    CreateRequisicaoCompraCommand toCommand(CreateRequisicaoCompraRequest request);

    @Mapping(target = "uuid", source = "id")
    RequisicaoCompraResponse toResponse(RequisicaoCompra requisicao);

    ItemRequisicaoCompraResponse toItemResponse(ItemRequisicaoCompra item);

    default RequisicaoCompraPageResponse toPageResponse(PageResult<RequisicaoCompra> page) {
        return new RequisicaoCompraPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
