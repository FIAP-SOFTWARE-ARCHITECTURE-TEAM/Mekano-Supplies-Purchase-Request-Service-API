package com.fiap.mekano.supplies.domain.model;

public enum MotivoRequisicao {
    ESTOQUE_MINIMO("Estoque Minimo"),
    ORDEM_SERVICO("Ordem de Servico");

    private final String descricao;

    MotivoRequisicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
