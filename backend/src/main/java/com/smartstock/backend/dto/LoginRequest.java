package com.smartstock.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * <b>Em palavras simples:</b> Formato da primeira etapa do login: nome de usuário e senha.
 */
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String senha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
