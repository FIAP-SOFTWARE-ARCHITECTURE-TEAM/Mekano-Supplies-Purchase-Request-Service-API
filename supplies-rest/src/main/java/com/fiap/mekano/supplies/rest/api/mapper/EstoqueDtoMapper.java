package com.fiap.mekano.supplies.rest.api.mapper;

import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.port.in.MovimentarEstoqueCommand;
import com.fiap.mekano.supplies.domain.port.in.MovimentoEstoqueResult;
import com.fiap.mekano.supplies.rest.api.dto.EstoquePageResponse;
import com.fiap.mekano.supplies.rest.api.dto.EstoqueResponse;
import com.fiap.mekano.supplies.rest.api.dto.MovimentarEstoqueRequest;
import com.fiap.mekano.supplies.rest.api.dto.MovimentoEstoqueResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EstoqueDtoMapper {

    public MovimentarEstoqueCommand toCommand(MovimentarEstoqueRequest request) {
        if (request == null) {
            return null;
        }
        return new MovimentarEstoqueCommand(
                request.getPecaUuid(),
                request.getTipo(),
                request.getQuantidade(),
                request.getOrigem(),
                request.getReferenciaUuid(),
                request.getIdempotencyKey());
    }

    public EstoqueResponse toResponse(Estoque estoque) {
        if (estoque == null) {
            return null;
        }
        return new EstoqueResponse(
                estoque.getId(),
                estoque.getPecaUuid(),
                estoque.getSaldoAtual(),
                estoque.getSaldoReservado(),
                estoque.disponivel(),
                estoque.getEstoqueMinimo(),
                estoque.getVersion(),
                estoque.getCreatedAt(),
                estoque.getUpdatedAt());
    }

    public EstoquePageResponse toPageResponse(PageResult<Estoque> page) {
        return new EstoquePageResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public MovimentoEstoqueResponse toMovementResponse(MovimentoEstoqueResult result) {
        var movimento = result.movimento();
        return new MovimentoEstoqueResponse(
                movimento.getId(),
                movimento.getPecaUuid(),
                movimento.getTipo(),
                movimento.getQuantidade(),
                movimento.getSaldoAtualApos(),
                movimento.getSaldoReservadoApos(),
                movimento.getOrigem(),
                movimento.getReferenciaUuid(),
                movimento.getIdempotencyKey(),
                movimento.getCreatedAt(),
                result.idempotente());
    }
}
