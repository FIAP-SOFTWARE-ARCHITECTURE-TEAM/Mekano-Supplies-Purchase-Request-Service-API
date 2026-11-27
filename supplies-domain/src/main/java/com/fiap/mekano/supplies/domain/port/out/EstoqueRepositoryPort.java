package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.Estoque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstoqueRepositoryPort {

    Estoque findOrCreateAndLock(UUID pecaUuid);

    Optional<Estoque> findByPecaUuid(UUID pecaUuid);

    Estoque save(Estoque estoque);

    List<Estoque> findAbaixoMinimo(int page, int size);

    long countAbaixoMinimo();
}
