package com.smartstock.backend.dto;

import java.time.LocalDate;

/**
 * <b>Em palavras simples:</b> Formato dos dados para mudar as datas de um pedido no Calendário (data do pedido e meta de entrega).
 *
 * DTO ("objeto de transferência de dados") é o "formulário" que a API aceita ou devolve: diz quais campos
 * existem e quais regras eles têm (ex.: @NotNull = obrigatório). Não é tabela do banco.
 */
public class CalendarioUpdateRequest {

    private LocalDate dataPedido;

    private LocalDate metaEntrega;

    public LocalDate getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDate dataPedido) {
        this.dataPedido = dataPedido;
    }

    public LocalDate getMetaEntrega() {
        return metaEntrega;
    }

    public void setMetaEntrega(LocalDate metaEntrega) {
        this.metaEntrega = metaEntrega;
    }
}
