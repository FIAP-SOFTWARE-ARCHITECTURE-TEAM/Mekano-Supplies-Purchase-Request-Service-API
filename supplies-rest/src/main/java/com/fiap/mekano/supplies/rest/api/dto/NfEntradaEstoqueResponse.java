package com.fiap.mekano.supplies.rest.api.dto;

import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;

import java.util.UUID;

public record NfEntradaEstoqueResponse(
        UUID movimentoUuid,
        UUID pecaUuid,
        TipoMovimentoEstoque tipo,
        OrigemMovimentoEstoque origem,
        Long quantidade,
        long saldoAtualApos,
        long saldoReservadoApos,
        String idempotencyKey
) {
}
