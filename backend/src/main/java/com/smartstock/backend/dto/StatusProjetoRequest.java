package com.smartstock.backend.dto;

import com.smartstock.backend.model.StatusProjeto;

import jakarta.validation.constraints.NotNull;

/**
 * <b>Em palavras simples:</b> Formato do pedido para mudar a etapa de um projeto (ex.: de AGUARDANDO para EM_PRODUCAO).
 */
public class StatusProjetoRequest {

    @NotNull
    private StatusProjeto status;

    public StatusProjeto getStatus() {
        return status;
    }

    public void setStatus(StatusProjeto status) {
        this.status = status;
    }
}
