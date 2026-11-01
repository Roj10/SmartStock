package com.smartstock.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * <b>Em palavras simples:</b> Tabela checklist_itens: cada etapa do checklist de um projeto (ex.: "Cortar os tubos"), em ordem,
 * marcada ou não como concluída.
 *
 * As classes da pasta "model" são as tabelas do banco escritas em Java: cada campo é uma coluna
 * (@Column), e @ManyToOne/@OneToMany ligam uma tabela à outra.
 */
@Entity
@Table(name = "checklist_itens")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    @JsonIgnore
    private Projeto projeto;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private boolean concluido = false;

    @Column(nullable = false)
    private int ordem;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public void setProjeto(Projeto projeto) {
        this.projeto = projeto;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public boolean isConcluido() {
        return concluido;
    }

    public void setConcluido(boolean concluido) {
        this.concluido = concluido;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }
}
