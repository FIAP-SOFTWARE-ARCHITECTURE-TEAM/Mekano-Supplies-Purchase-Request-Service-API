package com.fiap.mekano.supplies.infrastructure.client;

import com.fiap.mekano.supplies.infrastructure.client.dto.RegistryPecaBatchRequest;
import com.fiap.mekano.supplies.infrastructure.client.dto.RegistryPecaBatchResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "registry-api")
@Path("/api/v4/internal/pecas")
public interface RegistryPecaRestClient {

    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    RegistryPecaBatchResponse buscarPorUuids(RegistryPecaBatchRequest request);
}
