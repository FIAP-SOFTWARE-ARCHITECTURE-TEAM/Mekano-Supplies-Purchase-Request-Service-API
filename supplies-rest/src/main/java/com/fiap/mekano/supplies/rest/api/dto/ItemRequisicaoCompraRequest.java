package com.fiap.mekano.supplies.rest.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ItemRequisicaoCompraRequest {

    @NotNull(message = "O UUID da peca e obrigatorio")
    private UUID pecaUuid;

    @NotNull(message = "A quantidade e obrigatoria")
    @Positive(message = "A quantidade deve ser maior que zero")
    private Long quantidade;
}
