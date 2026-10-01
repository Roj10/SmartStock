package com.smartstock.backend.dto;

import com.smartstock.backend.model.StatusProjeto;

import jakarta.validation.constraints.NotNull;

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
