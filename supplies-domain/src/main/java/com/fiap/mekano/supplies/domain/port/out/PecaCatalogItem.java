package com.fiap.mekano.supplies.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot de uma peca retornado pelo catalogo do Registry.
 */
public record PecaCatalogItem(
        UUID uuid,
        String codigo,
        String descricao,
        BigDecimal valorUnitario,
        boolean ativo
) {
}
