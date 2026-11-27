package com.fiap.mekano.supplies.domain.model;

import java.util.List;

/**
 * Resultado de uma NF com os itens da requisicao e os movimentos de estoque gerados.
 */
public record NfEntradaDetalhe(
        NfEntrada nfEntrada,
        List<ItemRequisicaoCompra> itens,
        List<MovimentoEstoque> movimentos
) {

    public NfEntradaDetalhe {
        if (itens == null) {
            itens = List.of();
        }
        if (movimentos == null) {
            movimentos = List.of();
        }
        itens = List.copyOf(itens);
        movimentos = List.copyOf(movimentos);
    }
}
