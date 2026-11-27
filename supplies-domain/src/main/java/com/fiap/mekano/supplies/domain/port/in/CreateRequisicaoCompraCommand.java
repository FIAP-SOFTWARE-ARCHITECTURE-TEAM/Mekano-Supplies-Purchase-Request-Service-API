package com.fiap.mekano.supplies.domain.port.in;

import java.util.List;

public record CreateRequisicaoCompraCommand(List<ItemRequisicaoCompraCommand> itens, String observacao) {

    public CreateRequisicaoCompraCommand {
        itens = itens == null ? null : List.copyOf(itens);
    }
}
