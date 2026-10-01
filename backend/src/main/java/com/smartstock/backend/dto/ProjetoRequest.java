package com.smartstock.backend.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class ProjetoRequest {

    @NotBlank
    private String nome;

    private String descricao;

    @Valid
    private List<ChecklistItemRequest> checklist = new ArrayList<>();

    @Valid
    private List<ProjetoMaterialRequest> materiais = new ArrayList<>();

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

    public List<ChecklistItemRequest> getChecklist() {
        return checklist;
    }

    public void setChecklist(List<ChecklistItemRequest> checklist) {
        this.checklist = checklist;
    }

    public List<ProjetoMaterialRequest> getMateriais() {
        return materiais;
    }

    public void setMateriais(List<ProjetoMaterialRequest> materiais) {
        this.materiais = materiais;
    }
}
