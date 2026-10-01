package com.smartstock.backend.dto;

import java.util.Set;

public class LoginResponse {

    private String token;
    private String username;
    private String nome;
    private String role;
    private Set<String> permissoes;

    public LoginResponse(String token, String username, String nome, String role, Set<String> permissoes) {
        this.token = token;
        this.username = username;
        this.nome = nome;
        this.role = role;
        this.permissoes = permissoes;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getNome() {
        return nome;
    }

    public String getRole() {
        return role;
    }

    public Set<String> getPermissoes() {
        return permissoes;
    }
}
