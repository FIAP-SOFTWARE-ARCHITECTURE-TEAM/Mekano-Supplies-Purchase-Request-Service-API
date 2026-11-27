package com.fiap.mekano.supplies.rest.api;

import com.fiap.mekano.supplies.domain.port.in.EstoqueServicePort;
import com.fiap.mekano.supplies.rest.api.dto.EstoquePageResponse;
import com.fiap.mekano.supplies.rest.api.dto.EstoqueResponse;
import com.fiap.mekano.supplies.rest.api.dto.MovimentarEstoqueRequest;
import com.fiap.mekano.supplies.rest.api.dto.MovimentoEstoqueResponse;
import com.fiap.mekano.supplies.rest.api.exception.ProblemDetail;
import com.fiap.mekano.supplies.rest.api.mapper.EstoqueDtoMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/internal/estoque")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Estoque interno", description = "Consulta e movimentacao interna do estoque")
public class EstoqueResource {

    private final EstoqueServicePort service;
    private final EstoqueDtoMapper mapper;

    @Inject
    public EstoqueResource(EstoqueServicePort service, EstoqueDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // TODO: habilitar autenticacao entre servicos
    // @RolesAllowed("service:estoque")
    @POST
    @Path("/movimentos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar movimento de estoque idempotente")
    @APIResponse(responseCode = "201", description = "Movimento aplicado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MovimentoEstoqueResponse.class)))
    @APIResponse(responseCode = "200", description = "Movimento repetido; saldo nao alterado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = MovimentoEstoqueResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Saldo insuficiente ou conflito de idempotencia",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response movimentar(@Valid MovimentarEstoqueRequest request) {
        var result = service.movimentar(mapper.toCommand(request));
        int status = result.idempotente() ? 200 : 201;
        return Response.status(status).entity(mapper.toMovementResponse(result)).build();
    }

    // TODO: habilitar autenticacao entre servicos
    // @RolesAllowed("service:estoque")
    @GET
    @Path("/{pecaUuid}")
    @Operation(summary = "Consultar saldo de uma peca")
    @APIResponse(responseCode = "200", description = "Saldo encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstoqueResponse.class)))
    public EstoqueResponse buscar(@PathParam("pecaUuid") UUID pecaUuid) {
        return mapper.toResponse(service.buscar(pecaUuid));
    }

    // TODO: habilitar autenticacao entre servicos
    // @RolesAllowed("service:estoque")
    @GET
    @Path("/abaixo-minimo")
    @Operation(summary = "Listar estoque disponivel abaixo do minimo")
    @APIResponse(responseCode = "200", description = "Lista paginada de estoque abaixo do minimo",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = EstoquePageResponse.class)))
    public EstoquePageResponse listarAbaixoMinimo(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return mapper.toPageResponse(service.listarAbaixoMinimo(page, size));
    }
}
