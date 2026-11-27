package com.fiap.mekano.supplies.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "movimentos_estoque",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_movimentos_estoque_idempotency",
                columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
public class MovimentoEstoqueEntity extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "peca_uuid", nullable = false)
    private UUID pecaUuid;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "quantidade", nullable = false)
    private long quantidade;

    @Column(name = "saldo_atual_apos", nullable = false)
    private long saldoAtualApos;

    @Column(name = "saldo_reservado_apos", nullable = false)
    private long saldoReservadoApos;

    @Column(name = "origem", nullable = false, length = 40)
    private String origem;

    @Column(name = "referencia_uuid")
    private UUID referenciaUuid;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;
}
