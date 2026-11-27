package com.fiap.mekano.supplies.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateNfEntradaCommand(
        UUID requisicaoUuid,
        String chaveAcesso,
        BigDecimal valorTotal
) {
}
