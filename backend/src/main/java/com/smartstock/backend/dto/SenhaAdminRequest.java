package com.smartstock.backend.dto;

/** Corpo das ações que exigem a senha de um administrador (ex.: excluir um projeto). */
public class SenhaAdminRequest {

    private String senhaAdmin;

    public String getSenhaAdmin() {
        return senhaAdmin;
    }

    public void setSenhaAdmin(String senhaAdmin) {
        this.senhaAdmin = senhaAdmin;
    }
}
