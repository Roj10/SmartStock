package com.smartstock.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <b>Em palavras simples:</b> Formato dos dados para criar ou editar um plano/venda: cliente, projeto (opcional), descrição e valor.
 */
public class VendaRequest {

    @NotBlank
    private String cliente;

    private Long projetoId;

    private String descricao;

    @NotNull
    @DecimalMin(value = "0.00", message = "O valor não pode ser negativo")
    private BigDecimal valor;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public Long getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(Long projetoId) {
        this.projetoId = projetoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
