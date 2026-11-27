package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Requisicao de compra de pecas/insumos.
 *
 * <p>Regras da Sprint 1:</p>
 * <ul>
 *   <li>pelo menos 1 item;</li>
 *   <li>quantidade maior que zero (validada no item);</li>
 *   <li>sem peca duplicada;</li>
 *   <li>criacao publica sempre {@link StatusRequisicao#ABERTA} e
 *       {@link MotivoRequisicao#ESTOQUE_MINIMO}.</li>
 * </ul>
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class RequisicaoCompra {

    private final UUID id;
    private final List<ItemRequisicaoCompra> itens;
    private final StatusRequisicao status;
    private final MotivoRequisicao motivo;
    private final String observacao;
    private final LocalDateTime createdAt;

    public static RequisicaoCompra criar(List<ItemRequisicaoCompra> itens, String observacao) {
        validateItens(itens);

        return RequisicaoCompra.builder()
                .id(UUID.randomUUID())
                .itens(Collections.unmodifiableList(new ArrayList<>(itens)))
                .status(StatusRequisicao.ABERTA)
                .motivo(MotivoRequisicao.ESTOQUE_MINIMO)
                .observacao(observacao)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static RequisicaoCompra reconstitute(UUID id, List<ItemRequisicaoCompra> itens,
                                                StatusRequisicao status, MotivoRequisicao motivo,
                                                String observacao, LocalDateTime createdAt) {
        validateId(id);
        validateStatus(status);
        validateMotivo(motivo);
        validateItens(itens);

        return RequisicaoCompra.builder()
                .id(id)
                .itens(Collections.unmodifiableList(new ArrayList<>(itens)))
                .status(status)
                .motivo(motivo)
                .observacao(observacao)
                .createdAt(createdAt)
                .build();
    }

    private static void validateId(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("requisicao_compra.id.required"));
        }
    }

    private static void validateStatus(StatusRequisicao status) {
        if (status == null) {
            throw new AppException(400, Messages.get("requisicao_compra.status.required"));
        }
    }

    private static void validateMotivo(MotivoRequisicao motivo) {
        if (motivo == null) {
            throw new AppException(400, Messages.get("requisicao_compra.motivo.required"));
        }
    }

    private static void validateItens(List<ItemRequisicaoCompra> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new AppException(400, Messages.get("requisicao_compra.itens.required"));
        }

        Set<UUID> pecas = new HashSet<>();
        for (ItemRequisicaoCompra item : itens) {
            if (item == null) {
                throw new AppException(400, Messages.get("requisicao_compra.itens.required"));
            }
            if (!pecas.add(item.getPecaUuid())) {
                throw new AppException(400, Messages.get("requisicao_compra.itens.duplicados"));
            }
        }
    }
}
