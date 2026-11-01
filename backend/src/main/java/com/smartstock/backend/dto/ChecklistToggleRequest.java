package com.smartstock.backend.dto;

/**
 * <b>Em palavras simples:</b> Formato do pedido para marcar ou desmarcar uma etapa do checklist (concluido = true/false).
 */
public class ChecklistToggleRequest {

    private boolean concluido;

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }
}
