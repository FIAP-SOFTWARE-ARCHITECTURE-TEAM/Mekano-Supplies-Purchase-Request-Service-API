package com.fiap.mekano.supplies.rest.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateNfEntradaRequest {

    @NotNull(message = "O UUID da requisicao de compra e obrigatorio")
    private UUID requisicaoUuid;

    @NotBlank(message = "A chave de acesso da nota fiscal e obrigatoria")
    @Pattern(regexp = "\\d{44}", message = "A chave de acesso deve conter exatamente 44 digitos")
    private String chaveAcesso;

    @NotNull(message = "O valor total da nota fiscal e obrigatorio")
    @DecimalMin(value = "0.01", message = "O valor total da nota fiscal deve ser maior que zero")
    private BigDecimal valorTotal;
}
