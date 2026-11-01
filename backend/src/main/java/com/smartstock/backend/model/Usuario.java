package com.smartstock.backend.model;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permissoes", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false)
    private Set<Modulo> permissoes = new HashSet<>();

    /** 2FA (app autenticador): ativo depois que a pessoa escaneia o QR code e confirma um código. */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean doisFatoresAtivo = false;

    /** Segredo do app autenticador, cifrado (AES-GCM) com a chave do servidor. Nunca sai na API. */
    @JsonIgnore
    @Column(length = 200)
    private String totpSegredo;

    /** Segredo gerado durante a configuração, ainda não confirmado com um código. */
    @JsonIgnore
    @Column(length = 200)
    private String totpPendente;

    /** Último intervalo de 30 s já usado num login: impede reaproveitar o mesmo código. */
    @JsonIgnore
    @ColumnDefault("0")
    @Column(nullable = false)
    private long totpUltimoPasso = 0;

    /** Códigos de recuperação (uso único), guardados só como hash BCrypt. */
    @JsonIgnore
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "usuario_codigos_recuperacao", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "codigo_hash", nullable = false)
    private Set<String> codigosRecuperacao = new HashSet<>();

    /** Aumenta a cada troca de senha, saída ou redefinição do 2FA: derruba os logins abertos dessa conta. */
    @JsonIgnore
    @ColumnDefault("0")
    @Column(nullable = false)
    private int tokenVersao = 0;

    /** A pessoa precisa trocar a senha no próximo login (senha de exemplo ou definida pelo administrador). */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean trocarSenha = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.permissoes = permissoes != null ? permissoes : new HashSet<>();
    }

    public boolean isDoisFatoresAtivo() {
        return doisFatoresAtivo;
    }

    public void setDoisFatoresAtivo(boolean doisFatoresAtivo) {
        this.doisFatoresAtivo = doisFatoresAtivo;
    }

    public String getTotpSegredo() {
        return totpSegredo;
    }

    public void setTotpSegredo(String totpSegredo) {
        this.totpSegredo = totpSegredo;
    }

    public String getTotpPendente() {
        return totpPendente;
    }

    public void setTotpPendente(String totpPendente) {
        this.totpPendente = totpPendente;
    }

    public long getTotpUltimoPasso() {
        return totpUltimoPasso;
    }

    public void setTotpUltimoPasso(long totpUltimoPasso) {
        this.totpUltimoPasso = totpUltimoPasso;
    }

    public Set<String> getCodigosRecuperacao() {
        return codigosRecuperacao;
    }

    public int getTokenVersao() {
        return tokenVersao;
    }

    public void setTokenVersao(int tokenVersao) {
        this.tokenVersao = tokenVersao;
    }

    /** Derruba todas as sessões abertas dessa conta (os tokens antigos deixam de valer). */
    public void encerrarSessoes() {
        this.tokenVersao++;
    }

    public boolean isTrocarSenha() {
        return trocarSenha;
    }

    public void setTrocarSenha(boolean trocarSenha) {
        this.trocarSenha = trocarSenha;
    }

    /** Desliga o 2FA (redefinição pelo administrador): a pessoa configura de novo no próximo login. */
    public void limparDoisFatores() {
        this.doisFatoresAtivo = false;
        this.totpSegredo = null;
        this.totpPendente = null;
        this.totpUltimoPasso = 0;
        this.codigosRecuperacao.clear();
    }
}
