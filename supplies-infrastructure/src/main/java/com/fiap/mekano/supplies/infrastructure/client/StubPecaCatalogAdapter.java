package com.fiap.mekano.supplies.infrastructure.client;

import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Stub de desenvolvimento. Ativado apenas com {@code supplies.registry.stub=true}.
 * Util para subir o Supplies sem o Registry implementado.
 */
@Alternative
@Priority(1)
@ApplicationScoped
@IfBuildProperty(name = "supplies.registry.stub", stringValue = "true")
public class StubPecaCatalogAdapter implements PecaCatalogPort {

    @Override
    public PecaCatalogResult buscarPorUuids(Collection<UUID> pecaUuids) {
        if (pecaUuids == null || pecaUuids.isEmpty()) {
            return new PecaCatalogResult(List.of(), List.of());
        }

        List<PecaCatalogItem> pecas = pecaUuids.stream()
                .map(uuid -> new PecaCatalogItem(
                        uuid,
                        "PEA-STUB",
                        "Peca stub para desenvolvimento",
                        new BigDecimal("1.00"),
                        true))
                .toList();

        return new PecaCatalogResult(pecas, List.of());
    }
}
