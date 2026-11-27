package com.fiap.mekano.supplies.rest.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EstoqueResponse(
        UUID uuid,
        UUID pecaUuid,
        long saldoAtual,
        long saldoReservado,
        long disponivel,
        Long estoqueMinimo,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
