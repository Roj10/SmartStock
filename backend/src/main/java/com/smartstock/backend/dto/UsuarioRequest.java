package com.smartstock.backend.dto;

import java.util.Set;

import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <b>Em palavras simples:</b> Formato dos dados para criar ou editar uma conta: nome, usuário, senha, e-mail
 * (que recebe o código do login), perfil e abas permitidas.
 */
public class UsuarioRequest {

    @NotBlank
    private String nome;

    @NotBlank
    private String username;

    /**
     * Obrigatória ao criar um usuário novo; opcional ao editar (informe apenas
     * para redefinir a senha de um usuário existente).
     */
    @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
    private String senha;

    @NotNull
    private Role role;

    private Set<Modulo> permissoes;

    /**
     * E-mail que recebe o código de cada login (opcional). Se o administrador informar, a conta já começa com o
     * 2FA ativo nesse e-mail; se ficar vazio, a pessoa cadastra o próprio e-mail no primeiro login.
     * Ao editar: vazio apaga o e-mail (a pessoa cadastra de novo no próximo login).
     */
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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
