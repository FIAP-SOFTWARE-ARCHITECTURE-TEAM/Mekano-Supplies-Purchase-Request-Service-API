package com.fiap.mekano.supplies.rest.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemRequisicaoCompraResponse(
        UUID pecaUuid,
        String pecaCodigo,
        String pecaDescricao,
        Long quantidade,
        BigDecimal valorUnitario
) {
}
