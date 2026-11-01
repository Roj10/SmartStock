package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * <b>Em palavras simples:</b> Formato de um item do checklist ao criar ou editar um projeto (ex.: "Soldar a estrutura").
 */
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
