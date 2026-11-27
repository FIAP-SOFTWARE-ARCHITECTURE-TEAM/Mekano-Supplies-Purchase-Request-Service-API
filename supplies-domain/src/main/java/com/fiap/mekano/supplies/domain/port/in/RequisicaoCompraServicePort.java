package com.fiap.mekano.supplies.domain.port.in;

import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;
import com.fiap.mekano.supplies.domain.port.out.RequisicaoCompraFiltro;

import java.util.UUID;

public interface RequisicaoCompraServicePort {

    RequisicaoCompra criar(CreateRequisicaoCompraCommand command);

    RequisicaoCompra enviar(UUID id);

    RequisicaoCompra receber(UUID id);

    RequisicaoCompra cancelar(UUID id);

    RequisicaoCompra buscar(UUID id);

    PageResult<RequisicaoCompra> listar(RequisicaoCompraFiltro filtro, int page, int size);
}
