package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimentoEstoqueRepositoryPort {

    Optional<MovimentoEstoque> findByIdempotencyKey(String idempotencyKey);

    List<MovimentoEstoque> findByReferenciaUuid(UUID referenciaUuid);

    MovimentoEstoque save(MovimentoEstoque movimento);
}
