package com.fiap.mekano.supplies.domain.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da consulta em lote ao catalogo do Registry.
 *
 * @param pecas          pecas encontradas
 * @param naoEncontradas UUIDs solicitados que nao existem no catalogo
 */
public record PecaCatalogResult(List<PecaCatalogItem> pecas, List<UUID> naoEncontradas) {

    public PecaCatalogResult {
        pecas = pecas == null ? List.of() : List.copyOf(pecas);
        naoEncontradas = naoEncontradas == null ? List.of() : List.copyOf(naoEncontradas);
    }
}
