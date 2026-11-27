package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import com.fiap.mekano.supplies.domain.model.MovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.OrigemMovimentoEstoque;
import com.fiap.mekano.supplies.domain.model.TipoMovimentoEstoque;
import com.fiap.mekano.supplies.domain.port.out.MovimentoEstoqueRepositoryPort;
import com.fiap.mekano.supplies.infrastructure.entity.MovimentoEstoqueEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MovimentoEstoqueRepositoryImpl implements MovimentoEstoqueRepositoryPort {

    private final EntityManager entityManager;

    @Inject
    public MovimentoEstoqueRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<MovimentoEstoque> findByIdempotencyKey(String idempotencyKey) {
        return entityManager.createQuery("""
                        select movimento
                          from MovimentoEstoqueEntity movimento
                         where movimento.idempotencyKey = :idempotencyKey
                        """, MovimentoEstoqueEntity.class)
                .setParameter("idempotencyKey", idempotencyKey)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .map(MovimentoEstoqueRepositoryImpl::toDomain);
    }

    @Override
    public List<MovimentoEstoque> findByReferenciaUuid(UUID referenciaUuid) {
        if (referenciaUuid == null) {
            return List.of();
        }
        return entityManager.createQuery("""
                        select movimento
                          from MovimentoEstoqueEntity movimento
                         where movimento.referenciaUuid = :referenciaUuid
                         order by movimento.id
                        """, MovimentoEstoqueEntity.class)
                .setParameter("referenciaUuid", referenciaUuid)
                .getResultStream()
                .map(MovimentoEstoqueRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public MovimentoEstoque save(MovimentoEstoque movimento) {
        MovimentoEstoqueEntity entity = new MovimentoEstoqueEntity();
        entity.setUuid(movimento.getId());
        entity.setPecaUuid(movimento.getPecaUuid());
        entity.setTipo(movimento.getTipo().name());
        entity.setQuantidade(movimento.getQuantidade());
        entity.setSaldoAtualApos(movimento.getSaldoAtualApos());
        entity.setSaldoReservadoApos(movimento.getSaldoReservadoApos());
        entity.setOrigem(movimento.getOrigem().name());
        entity.setReferenciaUuid(movimento.getReferenciaUuid());
        entity.setIdempotencyKey(movimento.getIdempotencyKey());
        entity.setCreatedAt(movimento.getCreatedAt());
        try {
            entityManager.persist(entity);
            entityManager.flush();
        } catch (ConstraintViolationException ex) {
            String constraintName = ex.getConstraintName();
            if (constraintName != null
                    && constraintName.toLowerCase(Locale.ROOT).contains("ux_movimentos_estoque_idempotency")) {
                throw new AppException(409, Messages.get("estoque.idempotency.conflito"));
            }
            throw ex;
        }
        return toDomain(entity);
    }

    private static MovimentoEstoque toDomain(MovimentoEstoqueEntity entity) {
        return MovimentoEstoque.reconstitute(
                entity.getUuid(),
                entity.getPecaUuid(),
                TipoMovimentoEstoque.valueOf(entity.getTipo()),
                entity.getQuantidade(),
                entity.getSaldoAtualApos(),
                entity.getSaldoReservadoApos(),
                OrigemMovimentoEstoque.valueOf(entity.getOrigem()),
                entity.getReferenciaUuid(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt());
    }
}
