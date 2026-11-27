package com.fiap.mekano.supplies.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itens_requisicao_compra")
@Getter
@Setter
@NoArgsConstructor
public class ItemRequisicaoCompraEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "requisicao_uuid", nullable = false)
    private UUID requisicaoUuid;

    @Column(name = "peca_uuid", nullable = false)
    private UUID pecaUuid;

    @Column(name = "peca_codigo", nullable = false, length = 50)
    private String pecaCodigo;

    @Column(name = "peca_descricao", nullable = false, length = 255)
    private String pecaDescricao;

    @Column(name = "quantidade", nullable = false)
    private Long quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
