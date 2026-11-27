package com.fiap.mekano.supplies.domain.port.in;

import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;

import java.util.UUID;

public record MovimentarEstoqueCommand(
        UUID pecaUuid,
        TipoMovimentoEstoque tipo,
        Long quantidade,
        OrigemMovimentoEstoque origem,
        UUID referenciaUuid,
        String idempotencyKey
) {
}
