package com.fiap.mekano.supplies.rest.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateRequisicaoCompraRequest {

    @NotEmpty(message = "Pelo menos um item e obrigatorio")
    private List<@Valid ItemRequisicaoCompraRequest> itens;

    @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres")
    private String observacao;

    /**
     * Campo aceito por compatibilidade com o contrato. Na criacao publica
     * apenas ESTOQUE_MINIMO e permitido; o dominio sempre grava esse motivo.
     */
    private String motivo;
}
