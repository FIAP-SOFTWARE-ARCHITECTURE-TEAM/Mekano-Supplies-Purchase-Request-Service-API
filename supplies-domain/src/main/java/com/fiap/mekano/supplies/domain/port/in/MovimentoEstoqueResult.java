package com.fiap.mekano.supplies.domain.port.in;

import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;

public record MovimentoEstoqueResult(MovimentoEstoque movimento, boolean idempotente) {
}
