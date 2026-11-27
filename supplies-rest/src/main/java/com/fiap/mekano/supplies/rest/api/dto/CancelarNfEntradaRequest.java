package com.fiap.mekano.supplies.rest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelarNfEntradaRequest {

    @NotBlank(message = "O motivo do cancelamento e obrigatorio")
    @Size(max = 255, message = "O motivo do cancelamento deve ter no maximo 255 caracteres")
    private String motivo;
}
