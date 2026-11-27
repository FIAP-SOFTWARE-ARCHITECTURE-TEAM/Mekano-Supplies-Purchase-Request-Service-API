package com.fiap.mekano.supplies.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

public record RegistryPecaBatchRequest(List<UUID> pecaUuids) {
}
