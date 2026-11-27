package com.fiap.mekano.supplies.domain.port.in;

import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.model.NfEntradaDetalhe;
import com.fiap.mekano.supplies.domain.model.PageResult;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;

import java.util.UUID;

public interface NfEntradaServicePort {

    NfEntradaDetalhe registrar(CreateNfEntradaCommand command);

    PageResult<NfEntrada> listar(NfEntradaFiltro filtro, int page, int size);

    NfEntradaDetalhe buscar(UUID id);

    NfEntradaDetalhe cancelar(UUID id, String motivo);
}
