package com.fiap.mekano.supplies.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegistryPecaBatchResponse(
        List<RegistryPecaDto> pecas,
        List<UUID> naoEncontradas
) {
}
