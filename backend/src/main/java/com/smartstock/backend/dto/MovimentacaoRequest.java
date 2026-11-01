package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * <b>Em palavras simples:</b> Formato dos dados de uma entrada ou saída de estoque: item, quantidade e, na entrada, o fornecedor.
 */
public class MovimentacaoRequest {

    @NotNull
    private Long pastilhaId;

    @NotNull
    @Positive
    private Integer quantidade;

    private Long fornecedorId;

    private String observacao;

    public Long getPastilhaId() {
        return pastilhaId;
    }

    public void setPastilhaId(Long pastilhaId) {
        this.pastilhaId = pastilhaId;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Long getFornecedorId() {
        return fornecedorId;
    }

    public void setFornecedorId(Long fornecedorId) {
        this.fornecedorId = fornecedorId;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
