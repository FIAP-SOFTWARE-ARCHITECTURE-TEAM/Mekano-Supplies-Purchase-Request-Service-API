package com.fiap.mekano.supplies.rest.api;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;
import com.fiap.mekano.supplies.domain.port.in.RequisicaoCompraServicePort;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;
import com.fiap.mekano.supplies.rest.api.dto.CreateRequisicaoCompraRequest;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraPageResponse;
import com.fiap.mekano.supplies.rest.api.dto.RequisicaoCompraResponse;
import com.fiap.mekano.supplies.rest.api.exception.ProblemDetail;
import com.fiap.mekano.supplies.rest.api.mapper.RequisicaoCompraDtoMapper;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Path("/requisicoes-compra")
@RequestScoped
@Tag(name = "Requisicoes de Compra", description = "Criacao, listagem e consulta de requisicoes de compra")
public class RequisicaoCompraResource {

    private final RequisicaoCompraServicePort service;
    private final RequisicaoCompraDtoMapper mapper;

    @Inject
    public RequisicaoCompraResource(RequisicaoCompraServicePort service, RequisicaoCompraDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed({"admin", "financeiro"})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar requisicao de compra",
            description = "Cria uma requisicao ABERTA com motivo ESTOQUE_MINIMO e snapshots das pecas do Registry.")
    @APIResponse(responseCode = "201", description = "Requisicao criada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "Peca nao encontrada no Registry",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Peca inativa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "503", description = "Registry indisponivel",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public Response criar(@Valid CreateRequisicaoCompraRequest request, @Context UriInfo uriInfo) {
        validarMotivoPublico(request.getMotivo());
        RequisicaoCompra requisicao = service.criar(mapper.toCommand(request));
        URI location = uriInfo.getAbsolutePathBuilder().path(requisicao.getId().toString()).build();
        return Response.created(location).entity(mapper.toResponse(requisicao)).build();
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed({"admin", "financeiro"})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar requisicoes de compra",
            description = "Lista requisicoes com filtros opcionais e paginacao.")
    @APIResponse(responseCode = "200", description = "Lista paginada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = RequisicaoCompraPageResponse.class)))
    @APIResponse(responseCode = "400", description = "Filtro invalido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public Response listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("motivo") String motivo,
            @QueryParam("pecaUuid") UUID pecaUuid) {

        RequisicaoCompraFiltro filtro = new RequisicaoCompraFiltro(
                parseStatus(status), parseMotivo(motivo), pecaUuid);
        var resultado = service.listar(filtro, page, size);
        return Response.ok(mapper.toPageResponse(resultado)).build();
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed({"admin", "financeiro"})
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar requisicao de compra por UUID")
    @APIResponse(responseCode = "200", description = "Requisicao encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = RequisicaoCompraResponse.class)))
    @APIResponse(responseCode = "404", description = "Requisicao nao encontrada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public Response buscar(@PathParam("uuid") UUID uuid) {
        return Response.ok(mapper.toResponse(service.buscar(uuid))).build();
    }

    private static void validarMotivoPublico(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return;
        }
        if (!MotivoRequisicao.ESTOQUE_MINIMO.name().equalsIgnoreCase(motivo.trim())) {
            throw new AppException(400, Messages.get("requisicao_compra.motivo.invalido", motivo));
        }
    }

    private static StatusRequisicao parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusRequisicao.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(400, Messages.get("requisicao_compra.filtro.invalido", "status", status));
        }
    }

    private static MotivoRequisicao parseMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return null;
        }
        try {
            return MotivoRequisicao.valueOf(motivo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(400, Messages.get("requisicao_compra.filtro.invalido", "motivo", motivo));
        }
    }
}
