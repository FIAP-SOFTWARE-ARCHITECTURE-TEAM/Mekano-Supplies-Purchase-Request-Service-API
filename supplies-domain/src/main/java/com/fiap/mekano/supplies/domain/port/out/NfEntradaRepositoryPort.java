package com.fiap.mekano.supplies.domain.port.out;

import com.fiap.mekano.supplies.domain.model.NfEntrada;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NfEntradaRepositoryPort {

    NfEntrada save(NfEntrada nfEntrada);

    Optional<NfEntrada> findById(UUID id);

    Optional<NfEntrada> findByChaveAcesso(String chaveAcesso);

    List<NfEntrada> findAll(NfEntradaFiltro filtro, int page, int size);

    long count(NfEntradaFiltro filtro);
}
