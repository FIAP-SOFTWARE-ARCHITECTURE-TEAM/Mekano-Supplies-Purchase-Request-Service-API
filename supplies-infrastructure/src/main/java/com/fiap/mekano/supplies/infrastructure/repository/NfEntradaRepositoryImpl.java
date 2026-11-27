package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.NfEntrada;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaFiltro;
import com.fiap.mekano.supplies.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.supplies.infrastructure.entity.NfEntradaEntity;
import com.fiap.mekano.supplies.infrastructure.mapper.NfEntradaEntityMapper;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaRepositoryImpl implements NfEntradaRepositoryPort {

    private final NfEntradaPanacheRepository panache;
    private final NfEntradaEntityMapper mapper;
    private final EntityManager entityManager;

    @Inject
    public NfEntradaRepositoryImpl(NfEntradaPanacheRepository panache,
                                   NfEntradaEntityMapper mapper,
                                   EntityManager entityManager) {
        this.panache = panache;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public NfEntrada save(NfEntrada nfEntrada) {
        NfEntradaEntity entity = panache.find("uuid", nfEntrada.getId()).firstResult();

        try {
            if (entity == null) {
                entity = mapper.toEntity(nfEntrada);
                panache.persist(entity);
            } else {
                entity.setStatus(nfEntrada.getStatus().name());
                entity.setMotivoCancelamento(nfEntrada.getMotivoCancelamento());
                entity.setDataCancelamento(nfEntrada.getDataCancelamento());
            }
            entityManager.flush();
        } catch (ConstraintViolationException ex) {
            if (isChaveAcessoConstraint(ex)) {
                throw new AppException(409, Messages.get("nf_entrada.chave_acesso.duplicada"));
            }
            throw ex;
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<NfEntrada> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return panache.find("uuid", id).firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public Optional<NfEntrada> findByChaveAcesso(String chaveAcesso) {
        if (chaveAcesso == null || chaveAcesso.isBlank()) {
            return Optional.empty();
        }
        return panache.find("chaveAcesso", chaveAcesso.trim())
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public List<NfEntrada> findAll(NfEntradaFiltro filtro, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        String query = buildQuery(filtro, params);
        return panache.find(query + " order by n.id desc", params)
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count(NfEntradaFiltro filtro) {
        Map<String, Object> params = new HashMap<>();
        String query = buildQuery(filtro, params);
        return panache.count(query, params);
    }

    private static boolean isChaveAcessoConstraint(ConstraintViolationException ex) {
        String constraint = ex.getConstraintName() == null ? "" : ex.getConstraintName();
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        return containsIgnoreCase(constraint, "chave_acesso")
                || containsIgnoreCase(message, "ux_nf_entradas_chave_acesso");
    }

    private static boolean containsIgnoreCase(String texto, String trecho) {
        return texto.toLowerCase(Locale.ROOT).contains(trecho.toLowerCase(Locale.ROOT));
    }

    private static String buildQuery(NfEntradaFiltro filtro, Map<String, Object> params) {
        StringBuilder query = new StringBuilder("from NfEntradaEntity n where 1 = 1");

        if (filtro != null && filtro.status() != null) {
            query.append(" and n.status = :status");
            params.put("status", filtro.status().name());
        }
        if (filtro != null && filtro.requisicaoUuid() != null) {
            query.append(" and n.requisicaoUuid = :requisicaoUuid");
            params.put("requisicaoUuid", filtro.requisicaoUuid());
        }
        if (filtro != null && filtro.chaveAcesso() != null && !filtro.chaveAcesso().isBlank()) {
            query.append(" and n.chaveAcesso = :chaveAcesso");
            params.put("chaveAcesso", filtro.chaveAcesso().trim());
        }

        return query.toString();
    }
}
