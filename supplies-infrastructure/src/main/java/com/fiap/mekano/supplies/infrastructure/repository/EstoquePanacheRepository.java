package com.fiap.mekano.supplies.infrastructure.repository;

import com.fiap.mekano.supplies.infrastructure.entity.EstoqueEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EstoquePanacheRepository implements PanacheRepository<EstoqueEntity> {
}
