package com.smartstock.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class FornecedorProdutoItemRequest {

    @NotNull
    private Long pastilhaId;

    @NotNull
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
    private BigDecimal preco;

    public Long getPastilhaId() {
        return pastilhaId;
    }

    public void setPastilhaId(Long pastilhaId) {
        this.pastilhaId = pastilhaId;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }
}
