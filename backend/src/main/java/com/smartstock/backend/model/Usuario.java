package com.smartstock.backend.model;

import java.time.LocalDateTime;
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

/**
 * <b>Em palavras simples:</b> Tabela usuarios: as contas de acesso. Guarda nome, usuário, a senha (só o "hash", uma versão
 * embaralhada que não dá para desfazer), o perfil, as abas permitidas, o e-mail que recebe o código do 2FA
 * e a "versão do token", que permite encerrar todas as sessões da conta de uma vez.
 */
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

    /**
     * E-mail confirmado da pessoa: é para ele que vai o código de cada login (2FA por e-mail).
     * Enquanto estiver vazio, o próximo login pede para cadastrar um e-mail e confirmar com um código.
     */
    @Column(length = 254)
    private String email;

    /** E-mail informado no primeiro login, esperando a confirmação pelo código enviado a ele. */
    @JsonIgnore
    @Column(length = 254)
    private String emailPendente;

    /** Código de login enviado por e-mail, guardado só como "impressão digital" (HMAC), nunca o código em si. */
    @JsonIgnore
    @Column(length = 100)
    private String codigoEmailHash;

    /** Até quando o código enviado vale (10 minutos depois do envio). */
    @JsonIgnore
    private LocalDateTime codigoEmailExpira;

    /** Quando o último código foi enviado (para não deixar reenviar sem parar). */
    @JsonIgnore
    private LocalDateTime codigoEmailEnviado;

    /** Erros seguidos com o código atual: no 5º erro o código é cancelado e é preciso pedir outro. */
    @JsonIgnore
    @ColumnDefault("0")
    @Column(nullable = false)
    private int codigoEmailTentativas = 0;

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

    /** O 2FA está ativo quando a conta tem um e-mail confirmado (aparece como "doisFatoresAtivo" na API). */
    public boolean isDoisFatoresAtivo() {
        return email != null && !email.isBlank();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailPendente() {
        return emailPendente;
    }

    public void setEmailPendente(String emailPendente) {
        this.emailPendente = emailPendente;
    }

    public String getCodigoEmailHash() {
        return codigoEmailHash;
    }

    public LocalDateTime getCodigoEmailExpira() {
        return codigoEmailExpira;
    }

    public LocalDateTime getCodigoEmailEnviado() {
        return codigoEmailEnviado;
    }

    public int getCodigoEmailTentativas() {
        return codigoEmailTentativas;
    }

    public void setCodigoEmailTentativas(int codigoEmailTentativas) {
        this.codigoEmailTentativas = codigoEmailTentativas;
    }

    /** Guarda um código novo (já transformado em HMAC), válido por alguns minutos. */
    public void novoCodigoEmail(String hash, LocalDateTime agora, int minutosValidade) {
        this.codigoEmailHash = hash;
        this.codigoEmailEnviado = agora;
        this.codigoEmailExpira = agora.plusMinutes(minutosValidade);
        this.codigoEmailTentativas = 0;
    }

    /** Apaga o código atual (depois de usado, vencido ou de muitos erros): ele não vale mais. */
    public void cancelarCodigoEmail() {
        this.codigoEmailHash = null;
        this.codigoEmailExpira = null;
        this.codigoEmailTentativas = 0;
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

    /** Desliga o 2FA (redefinição pelo administrador): a pessoa cadastra e confirma um e-mail no próximo login. */
    public void limparDoisFatores() {
        this.email = null;
        this.emailPendente = null;
        cancelarCodigoEmail();
    }
}
