package com.fiap.mekano.supplies.domain.port.out;

import java.util.Collection;
import java.util.UUID;

/**
 * Porta de saida para consulta do catalogo de pecas no Registry.
 */
public interface PecaCatalogPort {

    PecaCatalogResult buscarPorUuids(Collection<UUID> pecaUuids);
}
