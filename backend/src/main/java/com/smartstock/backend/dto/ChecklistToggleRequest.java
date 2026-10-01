package com.smartstock.backend.dto;

public class ChecklistToggleRequest {

    private boolean concluido;

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }
}
