package com.fiap.mekano.supplies.rest.api.dto;

import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class MovimentarEstoqueRequest {

    @NotNull
    private UUID pecaUuid;

    @NotNull
    private TipoMovimentoEstoque tipo;

    @NotNull
    @Positive
    private Long quantidade;

    @NotNull
    private OrigemMovimentoEstoque origem;

    private UUID referenciaUuid;

    @NotBlank
    @Size(max = 200)
    private String idempotencyKey;
}
