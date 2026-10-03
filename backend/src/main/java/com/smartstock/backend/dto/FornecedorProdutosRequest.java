package com.smartstock.backend.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

public class FornecedorProdutosRequest {

    @Valid
    private List<FornecedorProdutoItemRequest> produtos = new ArrayList<>();

    public List<FornecedorProdutoItemRequest> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<FornecedorProdutoItemRequest> produtos) {
        this.produtos = produtos;
    }
}
