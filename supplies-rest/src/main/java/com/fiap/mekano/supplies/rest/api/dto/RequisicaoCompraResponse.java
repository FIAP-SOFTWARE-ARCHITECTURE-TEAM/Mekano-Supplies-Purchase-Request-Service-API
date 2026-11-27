package com.fiap.mekano.supplies.rest.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RequisicaoCompraResponse(
        UUID uuid,
        String status,
        String motivo,
        String observacao,
        List<ItemRequisicaoCompraResponse> itens,
        LocalDateTime createdAt
) {
}
