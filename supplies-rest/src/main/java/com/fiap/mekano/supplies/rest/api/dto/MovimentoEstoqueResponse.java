package com.fiap.mekano.supplies.rest.api.dto;

import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;

import java.time.LocalDateTime;
import java.util.UUID;

public record MovimentoEstoqueResponse(
        UUID uuid,
        UUID pecaUuid,
        TipoMovimentoEstoque tipo,
        long quantidade,
        long saldoAtualApos,
        long saldoReservadoApos,
        OrigemMovimentoEstoque origem,
        UUID referenciaUuid,
        String idempotencyKey,
        LocalDateTime createdAt,
        boolean idempotente
) {
}
