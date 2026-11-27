package com.fiap.mekano.supplies.rest.api.mapper;

import com.fiap.mekano.supplies.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.NfEntradaDetalhe;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.supplies.rest.api.dto.CreateNfEntradaRequest;
import com.fiap.mekano.supplies.rest.api.dto.ItemRequisicaoCompraResponse;
import com.fiap.mekano.supplies.rest.api.dto.NfEntradaEstoqueResponse;
import com.fiap.mekano.supplies.rest.api.dto.NfEntradaPageResponse;
import com.fiap.mekano.supplies.rest.api.dto.NfEntradaResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class NfEntradaDtoMapper {

    public CreateNfEntradaCommand toCommand(CreateNfEntradaRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateNfEntradaCommand(
                request.getRequisicaoUuid(),
                request.getChaveAcesso(),
                request.getValorTotal());
    }

    public NfEntradaResponse toResponse(NfEntradaDetalhe detalhe) {
        if (detalhe == null) {
            return null;
        }
        return toResponse(
                detalhe.nfEntrada(),
                detalhe.itens().stream().map(this::toItemResponse).toList(),
                detalhe.movimentos().stream().map(this::toEstoqueResponse).toList());
    }

    public NfEntradaResponse toResponse(NfEntrada nfEntrada) {
        if (nfEntrada == null) {
            return null;
        }
        return toResponse(nfEntrada, List.of(), List.of());
    }

    public NfEntradaPageResponse toPageResponse(PageResult<NfEntrada> page) {
        return new NfEntradaPageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    private NfEntradaResponse toResponse(NfEntrada nfEntrada,
                                         List<ItemRequisicaoCompraResponse> itens,
                                         List<NfEntradaEstoqueResponse> estoque) {
        return new NfEntradaResponse(
                nfEntrada.getId(),
                nfEntrada.getRequisicaoUuid(),
                nfEntrada.getChaveAcesso(),
                nfEntrada.getValorTotal(),
                nfEntrada.getStatus().name(),
                nfEntrada.getMotivoCancelamento(),
                nfEntrada.getDataCancelamento(),
                nfEntrada.getCreatedAt(),
                itens,
                estoque);
    }

    private ItemRequisicaoCompraResponse toItemResponse(ItemRequisicaoCompra item) {
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

    private NfEntradaEstoqueResponse toEstoqueResponse(MovimentoEstoque movimento) {
        if (movimento == null) {
            return null;
        }
        return new NfEntradaEstoqueResponse(
                movimento.getId(),
                movimento.getPecaUuid(),
                movimento.getTipo(),
                movimento.getOrigem(),
                movimento.getQuantidade(),
                movimento.getSaldoAtualApos(),
                movimento.getSaldoReservadoApos(),
                movimento.getIdempotencyKey());
    }
}
