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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RegistryPecaCatalogAdapter implements PecaCatalogPort {

    private final RegistryPecaRestClient client;

    public RegistryPecaCatalogAdapter(@RestClient RegistryPecaRestClient client) {
        this.client = client;
    }

    @Override
    public PecaCatalogResult buscarPorUuids(Collection<UUID> pecaUuids) {
        if (pecaUuids == null) {
            Log.warn("Consulta de catalogo recebeu lista de UUIDs nula");
            throw new AppException(400, Messages.get("registry.uuids.required"));
        }
        if (pecaUuids.isEmpty()) {
            return new PecaCatalogResult(List.of(), List.of());
        }

        if (pecaUuids.stream().anyMatch(Objects::isNull)) {
            Log.warn("Consulta de catalogo recebeu UUID nulo");
            throw new AppException(400, Messages.get("registry.uuids.required"));
        }

        List<UUID> requestedUuids = List.copyOf(new LinkedHashSet<>(pecaUuids));
        try {
            RegistryPecaBatchResponse response = client.buscarPorUuids(
                    new RegistryPecaBatchRequest(requestedUuids));
            if (response == null) {
                throw invalidRegistryResponse();
            }
            validateResponse(response, requestedUuids);

            List<PecaCatalogItem> pecas = response.pecas().stream().map(this::toItem).toList();

            return new PecaCatalogResult(pecas, response.naoEncontradas());
        } catch (WebApplicationException | ProcessingException ex) {
            Log.warnf("Falha ao consultar o catalogo do Registry: erro=%s", ex.getMessage());
            throw new AppException(503, Messages.get("registry.indisponivel"), ex);
        }
    }

    private void validateResponse(RegistryPecaBatchResponse response, List<UUID> requestedUuids) {
        if (response.pecas() == null || response.naoEncontradas() == null) {
            throw invalidRegistryResponse();
        }

        Set<UUID> requested = new HashSet<>(requestedUuids);
        Set<UUID> accountedFor = new HashSet<>();
        for (RegistryPecaDto peca : response.pecas()) {
            if (peca == null
                    || peca.uuid() == null
                    || !requested.contains(peca.uuid())
                    || !accountedFor.add(peca.uuid())
                    || peca.codigo() == null
                    || peca.codigo().isBlank()
                    || peca.descricao() == null
                    || peca.descricao().isBlank()
                    || peca.valorUnitario() == null
                    || peca.valorUnitario().signum() <= 0
                    || peca.isActive() == null) {
                throw invalidRegistryResponse();
            }
        }

        for (UUID uuid : response.naoEncontradas()) {
            if (uuid == null || !requested.contains(uuid) || !accountedFor.add(uuid)) {
                throw invalidRegistryResponse();
            }
        }

        if (accountedFor.size() != requested.size()) {
            throw invalidRegistryResponse();
        }
    }

    private AppException invalidRegistryResponse() {
        Log.warn("Resposta do batch do Registry invalida ou incompleta");
        return new AppException(503, Messages.get("registry.indisponivel"));
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
