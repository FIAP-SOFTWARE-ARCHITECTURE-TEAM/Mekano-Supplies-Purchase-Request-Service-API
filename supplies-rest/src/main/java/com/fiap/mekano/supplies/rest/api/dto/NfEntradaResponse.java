package com.fiap.mekano.supplies.rest.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record NfEntradaResponse(
        UUID uuid,
        UUID requisicaoUuid,
        String chaveAcesso,
        BigDecimal valorTotal,
        String status,
        String motivoCancelamento,
        LocalDateTime dataCancelamento,
        LocalDateTime createdAt,
        List<ItemRequisicaoCompraResponse> itens,
        List<NfEntradaEstoqueResponse> estoque
) {
}
