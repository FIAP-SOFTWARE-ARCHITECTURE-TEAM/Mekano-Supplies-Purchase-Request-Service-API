package com.fiap.mekano.supplies.infrastructure.client;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogItem;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogPort;
import com.fiap.mekano.supplies.domain.port.out.PecaCatalogResult;
import com.fiap.mekano.supplies.infrastructure.client.dto.RegistryPecaBatchRequest;
import com.fiap.mekano.supplies.infrastructure.client.dto.RegistryPecaBatchResponse;
import com.fiap.mekano.supplies.infrastructure.client.dto.RegistryPecaDto;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RegistryPecaCatalogAdapter implements PecaCatalogPort {

    private final RegistryPecaRestClient client;

    public RegistryPecaCatalogAdapter(@RestClient RegistryPecaRestClient client) {
        this.client = client;
    }

    @Override
    public PecaCatalogResult buscarPorUuids(Collection<UUID> pecaUuids) {
        if (pecaUuids == null || pecaUuids.isEmpty()) {
            return new PecaCatalogResult(List.of(), List.of());
        }

        try {
            RegistryPecaBatchResponse response = client.buscarPorUuids(
                    new RegistryPecaBatchRequest(List.copyOf(pecaUuids)));
            if (response == null) {
                Log.warn("Catalogo do Registry retornou corpo vazio");
                throw new AppException(503, Messages.get("registry.indisponivel"));
            }

            List<PecaCatalogItem> pecas = response.pecas() == null
                    ? List.of()
                    : response.pecas().stream().map(this::toItem).toList();
            List<UUID> naoEncontradas = response.naoEncontradas() == null
                    ? List.of()
                    : response.naoEncontradas();

            return new PecaCatalogResult(pecas, naoEncontradas);
        } catch (WebApplicationException | ProcessingException ex) {
            Log.warnf("Falha ao consultar o catalogo do Registry: erro=%s", ex.getMessage());
            throw new AppException(503, Messages.get("registry.indisponivel"), ex);
        }
    }

    private PecaCatalogItem toItem(RegistryPecaDto dto) {
        return new PecaCatalogItem(
                dto.uuid(),
                dto.codigo(),
                dto.descricao(),
                dto.valorUnitario(),
                Boolean.TRUE.equals(dto.isActive()));
    }
}
