package com.fiap.mekano.supplies.domain.port.in;

import java.util.UUID;

public record ItemRequisicaoCompraCommand(UUID pecaUuid, Long quantidade) {
}
