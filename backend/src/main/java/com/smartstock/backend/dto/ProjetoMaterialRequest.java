package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * <b>Em palavras simples:</b> Formato de um material usado no projeto: qual item e quantas unidades.
 */
public class ProjetoMaterialRequest {

    @NotNull
    private Long pastilhaId;

    @Positive
    private int quantidade;

    public Long getPastilhaId() {
        return pastilhaId;
    }

    public void setPastilhaId(Long pastilhaId) {
        this.pastilhaId = pastilhaId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
