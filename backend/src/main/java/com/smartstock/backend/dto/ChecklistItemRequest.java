package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ChecklistItemRequest {

    @NotBlank
    private String texto;

    private boolean concluido;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }
}
