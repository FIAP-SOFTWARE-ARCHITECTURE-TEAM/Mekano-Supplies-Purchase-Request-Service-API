package com.fiap.mekano.supplies.domain.model;

/**
 * Estados possiveis de uma requisicao de compra.
 *
 * <p>Sprint 1 cria sempre em {@link #ABERTA}. As transicoes de ciclo de vida
 * sao implementadas no Sprint 2.</p>
 */
public enum StatusRequisicao {
    ABERTA("Aberta", "Aguardando envio ao fornecedor"),
    ENVIADA("Enviada", "Pedido enviado ao fornecedor"),
    COMPRA_APROVADA("Compra Aprovada", "Compra aprovada"),
    PRODUTO_RECEBIDO("Produto Recebido", "Mercadoria recebida"),
    CANCELADA("Cancelada", "Requisicao cancelada");

    private final String descricao;
    private final String mensagem;

    StatusRequisicao(String descricao, String mensagem) {
        this.descricao = descricao;
        this.mensagem = mensagem;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMensagem() {
        return mensagem;
    }
}
