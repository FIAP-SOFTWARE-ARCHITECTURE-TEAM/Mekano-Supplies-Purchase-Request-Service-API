package com.fiap.mekano.supplies.rest.api.mapper;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.supplies.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.supplies.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.supplies.rest.api.dto.ItemRequisicaoCompraResponse;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraPageResponse;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequisicaoCompraDtoMapper {

    public CreateRequisicaoCompraCommand toCommand(CreateRequisicaoCompraRequest request) {
        if (request == null) {
            return null;
        }
        var itens = request.getItens() == null
                ? null
                : request.getItens().stream()
                        .map(item -> item == null ? null
                                : new ItemRequisicaoCompraCommand(item.getPecaUuid(), item.getQuantidade()))
                        .toList();
        return new CreateRequisicaoCompraCommand(itens, request.getObservacao());
    }

    public RequisicaoCompraResponse toResponse(RequisicaoCompra requisicao) {
        if (requisicao == null) {
            return null;
        }
        return new RequisicaoCompraResponse(
                requisicao.getId(),
                requisicao.getStatus().name(),
                requisicao.getMotivo().name(),
                requisicao.getObservacao(),
                requisicao.getItens().stream().map(this::toItemResponse).toList(),
                requisicao.getCreatedAt());
    }

    public ItemRequisicaoCompraResponse toItemResponse(ItemRequisicaoCompra item) {
        if (item == null) {
            return null;
        }
        return new ItemRequisicaoCompraResponse(
                item.getPecaUuid(),
                item.getPecaCodigo(),
                item.getPecaDescricao(),
                item.getQuantidade(),
                item.getValorUnitario());
    }

    public RequisicaoCompraPageResponse toPageResponse(PageResult<RequisicaoCompra> page) {
        return new RequisicaoCompraPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
