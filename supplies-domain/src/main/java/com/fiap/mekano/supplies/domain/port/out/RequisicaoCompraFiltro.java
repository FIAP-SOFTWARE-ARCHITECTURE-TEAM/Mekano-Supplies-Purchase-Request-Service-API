package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.MotivoRequisicao;
import com.fiap.mekano.supplies.domain.model.StatusRequisicao;

import java.util.UUID;

/**
 * Filtros de listagem de requisicoes de compra.
 */
public record RequisicaoCompraFiltro(StatusRequisicao status, MotivoRequisicao motivo, UUID pecaUuid) {

    public static RequisicaoCompraFiltro vazio() {
        return new RequisicaoCompraFiltro(null, null, null);
    }
}
