package com.smartstock.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "projetos")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    private String cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProjeto status = StatusProjeto.AGUARDANDO;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    private LocalDateTime dataInicioProducao;

    private LocalDateTime dataFinalizacaoProducao;

    private LocalDate dataPedido;

    private LocalDate metaEntrega;

    private LocalDateTime dataEntrega;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ordem ASC")
    private List<ChecklistItem> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProjetoMaterial> materiais = new ArrayList<>();

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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public StatusProjeto getStatus() {
        return status;
    }

    public void setStatus(StatusProjeto status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataInicioProducao() {
        return dataInicioProducao;
    }

    public void setDataInicioProducao(LocalDateTime dataInicioProducao) {
        this.dataInicioProducao = dataInicioProducao;
    }

    public LocalDateTime getDataFinalizacaoProducao() {
        return dataFinalizacaoProducao;
    }

    public void setDataFinalizacaoProducao(LocalDateTime dataFinalizacaoProducao) {
        this.dataFinalizacaoProducao = dataFinalizacaoProducao;
    }

    public LocalDate getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDate dataPedido) {
        this.dataPedido = dataPedido;
    }

    public LocalDate getMetaEntrega() {
        return metaEntrega;
    }

    public void setMetaEntrega(LocalDate metaEntrega) {
        this.metaEntrega = metaEntrega;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDateTime dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public List<ChecklistItem> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<ChecklistItem> checklist) {
        this.checklist = checklist;
    }

    public List<ProjetoMaterial> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<ProjetoMaterial> materiais) {
        this.materiais = materiais;
    }

    public int getTotalChecklist() {
        return checklist.size();
    }

    public long getConcluidosChecklist() {
        return checklist.stream().filter(ChecklistItem::isConcluido).count();
    }

    public void ordenarChecklist() {
        checklist.sort(Comparator.comparingInt(ChecklistItem::getOrdem));
    }
}
