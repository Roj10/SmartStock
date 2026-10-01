package com.smartstock.backend.dto;

import java.util.Set;

import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioRequest {

    @NotBlank
    private String nome;

    @NotBlank
    private String username;

    /**
     * Obrigatória ao criar um usuário novo; opcional ao editar (informe apenas
     * para redefinir a senha de um usuário existente).
     */
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String senha;

    @NotNull
    private Role role;

    private Set<Modulo> permissoes;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<Modulo> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(Set<Modulo> permissoes) {
        this.permissoes = permissoes;
    }
}
