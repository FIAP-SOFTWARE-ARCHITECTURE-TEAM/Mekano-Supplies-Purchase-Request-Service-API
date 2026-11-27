package com.fiap.mekano.supplies.rest.api;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.StatusNfEntrada;
import com.fiap.mekano.supplies.domain.port.in.NfEntradaServicePort;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;
import com.fiap.mekano.supplies.rest.api.dto.CancelarNfEntradaRequest;
import com.fiap.mekano.supplies.rest.api.dto.CreateNfEntradaRequest;
import com.fiap.mekano.supplies.rest.api.dto.NfEntradaPageResponse;
import com.fiap.mekano.supplies.rest.api.dto.NfEntradaResponse;
import com.fiap.mekano.supplies.rest.api.exception.ProblemDetail;
import com.fiap.mekano.supplies.rest.api.mapper.NfEntradaDtoMapper;
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

@Path("/nf-entrada")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "NF de entrada", description = "Registro, consulta e cancelamento com estorno de estoque")
public class NfEntradaResource {

    private final NfEntradaServicePort service;
    private final NfEntradaDtoMapper mapper;

    @Inject
    public NfEntradaResource(NfEntradaServicePort service, NfEntradaDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed("admin")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Registrar nota fiscal de entrada",
            description = "Registra a NF e credita o estoque na mesma transacao local.")
    @APIResponse(responseCode = "201", description = "NF registrada e estoque creditado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NfEntradaResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados invalidos",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "Requisicao de compra nao encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "Chave duplicada ou requisicao em status invalido",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public Response registrar(@Valid CreateNfEntradaRequest request, @Context UriInfo uriInfo) {
        var detalhe = service.registrar(mapper.toCommand(request));
        URI location = uriInfo.getAbsolutePathBuilder().path(detalhe.nfEntrada().getId().toString()).build();
        return Response.created(location).entity(mapper.toResponse(detalhe)).build();
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed("admin")
    @GET
    @Operation(summary = "Listar notas fiscais de entrada",
            description = "Lista NFs com filtros opcionais e paginacao.")
    @APIResponse(responseCode = "200", description = "Lista paginada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NfEntradaPageResponse.class)))
    @APIResponse(responseCode = "400", description = "Filtro invalido",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public NfEntradaPageResponse listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @QueryParam("requisicaoUuid") UUID requisicaoUuid,
            @QueryParam("chaveAcesso") String chaveAcesso) {

        NfEntradaFiltro filtro = new NfEntradaFiltro(
                parseStatus(status), requisicaoUuid, validarChaveAcessoFiltro(chaveAcesso));
        return mapper.toPageResponse(service.listar(filtro, page, size));
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed("admin")
    @GET
    @Path("/{uuid}")
    @Operation(summary = "Buscar nota fiscal de entrada por UUID",
            description = "Retorna a NF, os itens da requisicao e os movimentos de estoque gerados.")
    @APIResponse(responseCode = "200", description = "NF encontrada",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NfEntradaResponse.class)))
    @APIResponse(responseCode = "404", description = "NF nao encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public NfEntradaResponse buscar(@PathParam("uuid") UUID uuid) {
        return mapper.toResponse(service.buscar(uuid));
    }

    // TODO: habilitar quando autenticacao entrar no escopo
    // @RolesAllowed("admin")
    @POST
    @Path("/{uuid}/cancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cancelar nota fiscal de entrada",
            description = "Estorna o estoque e cancela a NF na mesma transacao local.")
    @APIResponse(responseCode = "200", description = "NF cancelada e estoque estornado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = NfEntradaResponse.class)))
    @APIResponse(responseCode = "400", description = "Motivo do cancelamento invalido",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "404", description = "NF nao encontrada",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "NF ja cancelada ou saldo insuficiente para estorno",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public NfEntradaResponse cancelar(@PathParam("uuid") UUID uuid,
                                      @Valid CancelarNfEntradaRequest request) {
        String motivo = request == null ? null : request.getMotivo();
        return mapper.toResponse(service.cancelar(uuid, motivo));
    }

    private static StatusNfEntrada parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusNfEntrada.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(400, Messages.get("nf_entrada.filtro.invalido", "status", status));
        }
    }

    private static String validarChaveAcessoFiltro(String chaveAcesso) {
        if (chaveAcesso == null || chaveAcesso.isBlank()) {
            return null;
        }
        String chave = chaveAcesso.trim();
        if (!chave.matches("\\d{44}")) {
            throw new AppException(400, Messages.get("nf_entrada.filtro.invalido", "chaveAcesso", chaveAcesso));
        }
        return chave;
    }
}
