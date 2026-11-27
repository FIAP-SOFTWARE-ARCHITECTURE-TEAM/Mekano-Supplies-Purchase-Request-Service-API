package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;

import java.util.Optional;

public interface MovimentoEstoqueRepositoryPort {

    Optional<MovimentoEstoque> findByIdempotencyKey(String idempotencyKey);

    MovimentoEstoque save(MovimentoEstoque movimento);
}
