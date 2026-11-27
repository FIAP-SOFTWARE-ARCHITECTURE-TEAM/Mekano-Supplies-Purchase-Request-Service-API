package com.fiap.mekano.supplies.domain.port.in;

import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.domain.model.PageResult;

import java.util.UUID;

public interface EstoqueServicePort {

    Estoque obterOuCriar(UUID pecaUuid);

    Estoque buscar(UUID pecaUuid);

    PageResult<Estoque> listarAbaixoMinimo(int page, int size);

    MovimentoEstoqueResult movimentar(MovimentarEstoqueCommand command);
}
