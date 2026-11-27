package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.model.Estoque;
import com.fiap.mekano.supplies.domain.port.out.EstoqueRepositoryPort;
import com.fiap.mekano.supplies.infrastructure.entity.EstoqueEntity;
import com.fiap.mekano.supplies.infrastructure.mapper.EstoqueEntityMapper;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EstoqueRepositoryImpl implements EstoqueRepositoryPort {

    private final EstoquePanacheRepository panache;
    private final EstoqueEntityMapper mapper;
    private final EntityManager entityManager;
    private final String databaseKind;

    @Inject
    public EstoqueRepositoryImpl(EstoquePanacheRepository panache,
                                 EstoqueEntityMapper mapper,
                                 EntityManager entityManager,
                                 @ConfigProperty(name = "quarkus.datasource.db-kind") String databaseKind) {
        this.panache = panache;
        this.mapper = mapper;
        this.entityManager = entityManager;
        this.databaseKind = databaseKind;
    }

    @Override
    public Estoque findOrCreateAndLock(UUID pecaUuid) {
        if ("h2".equalsIgnoreCase(databaseKind)) {
            entityManager.createNativeQuery("""
                    MERGE INTO estoque AS e
                    USING (VALUES (CAST(:uuid AS UUID), CAST(:pecaUuid AS UUID))) AS v(uuid, peca_uuid)
                    ON e.peca_uuid = v.peca_uuid
                    WHEN NOT MATCHED THEN
                        INSERT (uuid, peca_uuid, saldo_atual, saldo_reservado, estoque_minimo, version, created_at)
                        VALUES (v.uuid, v.peca_uuid, 0, 0, NULL, 0, CURRENT_TIMESTAMP)
                    """)
                    .setParameter("uuid", UUID.randomUUID())
                    .setParameter("pecaUuid", pecaUuid)
                    .executeUpdate();
        } else {
            entityManager.createNativeQuery("""
                    INSERT INTO estoque (
                        uuid, peca_uuid, saldo_atual, saldo_reservado, estoque_minimo, version, created_at
                    )
                    VALUES (:uuid, :pecaUuid, 0, 0, NULL, 0, CURRENT_TIMESTAMP)
                    ON CONFLICT (peca_uuid) DO NOTHING
                    """)
                    .setParameter("uuid", UUID.randomUUID())
                    .setParameter("pecaUuid", pecaUuid)
                    .executeUpdate();
        }

        EstoqueEntity entity = panache.find("pecaUuid", pecaUuid)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
        if (entity == null) {
            throw new IllegalStateException("Estoque nao foi criado para a peca solicitada.");
        }
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Estoque> findByPecaUuid(UUID pecaUuid) {
        return panache.find("pecaUuid", pecaUuid).firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public Estoque save(Estoque estoque) {
        EstoqueEntity entity = panache.find("pecaUuid", estoque.getPecaUuid()).firstResult();
        if (entity == null) {
            throw new IllegalStateException("Estoque inexistente durante a atualizacao.");
        }
        entity.setSaldoAtual(estoque.getSaldoAtual());
        entity.setSaldoReservado(estoque.getSaldoReservado());
        entity.setEstoqueMinimo(estoque.getEstoqueMinimo());
        entity.setVersion(estoque.getVersion());
        return mapper.toDomain(entity);
    }

    @Override
    public List<Estoque> findAbaixoMinimo(int page, int size) {
        return panache.find("""
                estoqueMinimo is not null
                and saldoAtual - saldoReservado <= estoqueMinimo
                order by pecaUuid
                """)
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAbaixoMinimo() {
        return panache.count("""
                estoqueMinimo is not null
                and saldoAtual - saldoReservado <= estoqueMinimo
                """);
    }
}
