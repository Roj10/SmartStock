package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class PastilhaRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    private String descricao;

    private Long fabricanteId;

    @NotNull
    @PositiveOrZero
    private Integer estoqueMinimo;

    @PositiveOrZero
    private Integer quantidadeAtual;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getFabricanteId() {
        return fabricanteId;
    }

    public void setFabricanteId(Long fabricanteId) {
        this.fabricanteId = fabricanteId;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Integer estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public Integer getQuantidadeAtual() {
        return quantidadeAtual;
    }

    public void setQuantidadeAtual(Integer quantidadeAtual) {
        this.quantidadeAtual = quantidadeAtual;
    }
}
