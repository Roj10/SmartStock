package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MensagemRequest {

    @NotBlank(message = "Escreva uma mensagem")
    @Size(max = 2000, message = "A mensagem pode ter no máximo 2000 caracteres")
    private String texto;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
