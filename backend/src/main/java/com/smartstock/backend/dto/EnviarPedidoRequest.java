package com.smartstock.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public class EnviarPedidoRequest {

    @NotBlank(message = "Informe o nome do cliente para enviar o pedido")
    private String cliente;

    private LocalDate metaEntrega;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public LocalDate getMetaEntrega() {
        return metaEntrega;
    }

    public void setMetaEntrega(LocalDate metaEntrega) {
        this.metaEntrega = metaEntrega;
    }
}
