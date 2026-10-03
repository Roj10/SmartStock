package com.smartstock.backend.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/** Entrada de várias pastilhas de uma só vez (ex.: uma compra geral). */
public class EntradaLoteRequest {

    private String observacao;

    @Valid
    @NotEmpty(message = "Informe ao menos uma pastilha na entrada")
    private List<MovimentacaoRequest> itens = new ArrayList<>();

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<MovimentacaoRequest> getItens() {
        return itens;
    }

    public void setItens(List<MovimentacaoRequest> itens) {
        this.itens = itens;
    }
}
