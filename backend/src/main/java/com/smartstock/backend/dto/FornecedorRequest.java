package com.smartstock.backend.dto;

import com.smartstock.backend.model.TipoFornecedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FornecedorRequest {

    @NotBlank
    private String nome;

    @NotNull
    private TipoFornecedor tipo;

    private String contato;
    private String telefone;
    private String email;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoFornecedor getTipo() {
        return tipo;
    }

    public void setTipo(TipoFornecedor tipo) {
        this.tipo = tipo;
    }

    public String getContato() {
        return contato;
    }

    public void setContato(String contato) {
        this.contato = contato;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
