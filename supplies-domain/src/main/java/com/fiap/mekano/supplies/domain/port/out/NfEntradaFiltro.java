package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.StatusNfEntrada;

import java.util.UUID;

public record NfEntradaFiltro(
        StatusNfEntrada status,
        UUID requisicaoUuid,
        String chaveAcesso
) {

    public static NfEntradaFiltro vazio() {
        return new NfEntradaFiltro(null, null, null);
    }
}
