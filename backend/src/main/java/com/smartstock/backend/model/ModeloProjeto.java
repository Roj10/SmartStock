package com.smartstock.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Modelo (padrão) de projeto: etapas e materiais que se repetem a cada novo pedido do mesmo tipo
 * (ex.: "Cerca de metal"). Serve de ponto de partida para criar projetos novos.
 */
@Entity
@Table(name = "modelos_projeto")
public class ModeloProjeto {

    /** Material do modelo. Guarda só o id da pastilha: se ela for excluída, o item é removido do modelo. */
    @Embeddable
    public static class Material {

        @Column(name = "pastilha_id", nullable = false)
        private Long pastilhaId;

        @Column(nullable = false)
        private int quantidade;

        public Material() {
        }

        public Material(Long pastilhaId, int quantidade) {
            this.pastilhaId = pastilhaId;
            this.quantidade = quantidade;
        }

        public Long getPastilhaId() {
            return pastilhaId;
        }

        public int getQuantidade() {
            return quantidade;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @ElementCollection
    @CollectionTable(name = "modelo_projeto_etapas", joinColumns = @JoinColumn(name = "modelo_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "texto", nullable = false)
    private List<String> checklist = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "modelo_projeto_materiais", joinColumns = @JoinColumn(name = "modelo_id"))
    @OrderColumn(name = "ordem")
    private List<Material> materiais = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    public Long getId() {
        return id;
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

    public List<String> getChecklist() {
        return checklist;
    }

    public List<Material> getMateriais() {
        return materiais;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
