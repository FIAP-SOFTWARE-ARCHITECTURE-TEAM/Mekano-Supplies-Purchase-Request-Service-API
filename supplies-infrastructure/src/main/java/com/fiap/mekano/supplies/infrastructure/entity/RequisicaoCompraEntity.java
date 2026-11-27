package com.fiap.mekano.supplies.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "requisicoes_compra")
@Getter
@Setter
@NoArgsConstructor
public class RequisicaoCompraEntity extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "motivo", nullable = false, length = 30)
    private String motivo;

    @Column(name = "observacao", length = 255)
    private String observacao;
}
