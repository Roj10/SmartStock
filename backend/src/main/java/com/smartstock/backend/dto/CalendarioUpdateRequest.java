package com.smartstock.backend.dto;

import java.time.LocalDate;

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
