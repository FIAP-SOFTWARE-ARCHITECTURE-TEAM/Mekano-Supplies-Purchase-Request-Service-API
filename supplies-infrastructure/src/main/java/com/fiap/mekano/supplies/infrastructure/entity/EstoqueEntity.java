package com.fiap.mekano.supplies.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "estoque")
@Getter
@Setter
@NoArgsConstructor
public class EstoqueEntity extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "peca_uuid", nullable = false, unique = true)
    private UUID pecaUuid;

    @Column(name = "saldo_atual", nullable = false)
    private long saldoAtual;

    @Column(name = "saldo_reservado", nullable = false)
    private long saldoReservado;

    @Column(name = "estoque_minimo")
    private Long estoqueMinimo;

    @Column(name = "version", nullable = false)
    private long version;
}
