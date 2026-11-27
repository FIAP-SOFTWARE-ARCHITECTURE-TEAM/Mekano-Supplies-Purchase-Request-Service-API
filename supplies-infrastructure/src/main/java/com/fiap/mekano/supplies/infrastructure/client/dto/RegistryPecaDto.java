package com.fiap.mekano.supplies.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryPecaDto(
        UUID uuid,
        String codigo,
        String descricao,
        BigDecimal valorUnitario,
        Boolean isActive
) {
}
