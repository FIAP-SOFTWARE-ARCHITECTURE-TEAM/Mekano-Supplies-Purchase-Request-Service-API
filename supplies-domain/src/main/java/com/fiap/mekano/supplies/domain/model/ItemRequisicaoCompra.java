package com.fiap.mekano.supplies.domain.model;

import com.fiap.mekano.supplies.domain.exception.AppException;
import com.fiap.mekano.supplies.domain.exception.Messages;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Item de uma requisicao de compra.
 *
 * <p>Os campos {@code pecaCodigo}, {@code pecaDescricao} e {@code valorUnitario}
 * sao snapshots do catalogo do Registry no momento da criacao da requisicao.</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ItemRequisicaoCompra {

    private final UUID id;
    private final UUID pecaUuid;
    private final String pecaCodigo;
    private final String pecaDescricao;
    private final Long quantidade;
    private final BigDecimal valorUnitario;

    private ItemRequisicaoCompra(UUID id, UUID pecaUuid, String pecaCodigo, String pecaDescricao,
                                 Long quantidade, BigDecimal valorUnitario) {
        validateId(id);
        validatePecaUuid(pecaUuid);
        validateTexto(pecaCodigo, "requisicao_compra.peca_codigo.required");
        validateTexto(pecaDescricao, "requisicao_compra.peca_descricao.required");
        validateQuantidade(quantidade);
        validateValorUnitario(valorUnitario);

        this.id = id;
        this.pecaUuid = pecaUuid;
        this.pecaCodigo = pecaCodigo;
        this.pecaDescricao = pecaDescricao;
        this.quantidade = quantidade;
        this.valorUnitario = valorUnitario.setScale(2, RoundingMode.HALF_UP);
    }

    public static ItemRequisicaoCompra criar(UUID pecaUuid, String pecaCodigo, String pecaDescricao,
                                             Long quantidade, BigDecimal valorUnitario) {
        return new ItemRequisicaoCompra(UUID.randomUUID(), pecaUuid, pecaCodigo, pecaDescricao,
                quantidade, valorUnitario);
    }

    public static ItemRequisicaoCompra reconstitute(UUID id, UUID pecaUuid, String pecaCodigo,
                                                    String pecaDescricao, Long quantidade,
                                                    BigDecimal valorUnitario) {
        return new ItemRequisicaoCompra(id, pecaUuid, pecaCodigo, pecaDescricao, quantidade, valorUnitario);
    }

    private static void validateId(UUID id) {
        if (id == null) {
            throw new AppException(400, Messages.get("requisicao_compra.id.required"));
        }
    }

    private static void validatePecaUuid(UUID pecaUuid) {
        if (pecaUuid == null) {
            throw new AppException(400, Messages.get("requisicao_compra.peca.required"));
        }
    }

    private static void validateTexto(String valor, String chaveMensagem) {
        if (valor == null || valor.isBlank()) {
            throw new AppException(400, Messages.get(chaveMensagem));
        }
    }

    private static void validateQuantidade(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new AppException(400, Messages.get("requisicao_compra.quantidade.invalida"));
        }
    }

    private static void validateValorUnitario(BigDecimal valorUnitario) {
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, Messages.get("requisicao_compra.valor_unitario.invalido"));
        }
    }
}
