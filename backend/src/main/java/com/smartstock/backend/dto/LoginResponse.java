package com.smartstock.backend.dto;

public class LoginResponse {

    private String token;
    private String username;
    private String nome;
    private String role;

    public LoginResponse(String token, String username, String nome, String role) {
        this.token = token;
        this.username = username;
        this.nome = nome;
        this.role = role;
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
}
