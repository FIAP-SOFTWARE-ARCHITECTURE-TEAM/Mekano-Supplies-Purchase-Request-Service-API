package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.RequisicaoCompra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequisicaoCompraRepositoryPort {

    RequisicaoCompra save(RequisicaoCompra requisicao);

    Optional<RequisicaoCompra> findById(UUID id);

    List<RequisicaoCompra> findAll(RequisicaoCompraFiltro filtro, int page, int size);

    long count(RequisicaoCompraFiltro filtro);
}
