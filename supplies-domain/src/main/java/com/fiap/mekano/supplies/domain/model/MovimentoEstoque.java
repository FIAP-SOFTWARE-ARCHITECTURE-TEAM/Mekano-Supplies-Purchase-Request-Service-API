package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class MovimentoEstoque {

    private final UUID id;
    private final UUID pecaUuid;
    private final TipoMovimentoEstoque tipo;
    private final long quantidade;
    private final long saldoAtualApos;
    private final long saldoReservadoApos;
    private final OrigemMovimentoEstoque origem;
    private final UUID referenciaUuid;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;

    public static MovimentoEstoque criar(UUID pecaUuid, TipoMovimentoEstoque tipo, long quantidade,
                                         long saldoAtualApos, long saldoReservadoApos,
                                         OrigemMovimentoEstoque origem, UUID referenciaUuid,
                                         String idempotencyKey) {
        validar(pecaUuid, tipo, quantidade, saldoAtualApos, saldoReservadoApos, origem, idempotencyKey);
        return MovimentoEstoque.builder()
                .id(UUID.randomUUID())
                .pecaUuid(pecaUuid)
                .tipo(tipo)
                .quantidade(quantidade)
                .saldoAtualApos(saldoAtualApos)
                .saldoReservadoApos(saldoReservadoApos)
                .origem(origem)
                .referenciaUuid(referenciaUuid)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static MovimentoEstoque reconstitute(UUID id, UUID pecaUuid, TipoMovimentoEstoque tipo,
                                                long quantidade, long saldoAtualApos, long saldoReservadoApos,
                                                OrigemMovimentoEstoque origem, UUID referenciaUuid,
                                                String idempotencyKey, LocalDateTime createdAt) {
        if (id == null) {
            throw new AppException(400, Messages.get("estoque.movimento.uuid.required"));
        }
        validar(pecaUuid, tipo, quantidade, saldoAtualApos, saldoReservadoApos, origem, idempotencyKey);
        return MovimentoEstoque.builder()
                .id(id)
                .pecaUuid(pecaUuid)
                .tipo(tipo)
                .quantidade(quantidade)
                .saldoAtualApos(saldoAtualApos)
                .saldoReservadoApos(saldoReservadoApos)
                .origem(origem)
                .referenciaUuid(referenciaUuid)
                .idempotencyKey(idempotencyKey)
                .createdAt(createdAt)
                .build();
    }

    private static void validar(UUID pecaUuid, TipoMovimentoEstoque tipo, long quantidade,
                                long saldoAtualApos, long saldoReservadoApos,
                                OrigemMovimentoEstoque origem, String idempotencyKey) {
        if (pecaUuid == null) {
            throw new AppException(400, Messages.get("estoque.peca.required"));
        }
        if (tipo == null) {
            throw new AppException(400, Messages.get("estoque.tipo.required"));
        }
        if (quantidade <= 0) {
            throw new AppException(400, Messages.get("estoque.quantidade.invalida"));
        }
        if (saldoAtualApos < 0 || saldoReservadoApos < 0) {
            throw new AppException(400, Messages.get("estoque.saldo.invalido"));
        }
        if (origem == null) {
            throw new AppException(400, Messages.get("estoque.origem.required"));
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new AppException(400, Messages.get("estoque.idempotency.required"));
        }
    }
}
