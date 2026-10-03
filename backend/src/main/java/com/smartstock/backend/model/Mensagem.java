package com.smartstock.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Mensagem direta entre duas contas do sistema (cada conta representa um setor). */
@Entity
@Table(name = "mensagens", indexes = {
        @Index(name = "idx_mensagem_remetente", columnList = "remetente_id"),
        @Index(name = "idx_mensagem_destinatario", columnList = "destinatario_id") })
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Column(nullable = false, length = 2000)
    private String texto;

    /** Nome do arquivo da imagem anexada (guardado em pasta privada); nulo quando a mensagem é só texto. */
    private String imagemArquivo;

    private String imagemTipo;

    @Column(nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false)
    private boolean lida = false;

    public Long getId() {
        return id;
    }

    public Usuario getRemetente() {
        return remetente;
    }

    public void setRemetente(Usuario remetente) {
        this.remetente = remetente;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getImagemArquivo() {
        return imagemArquivo;
    }

    public void setImagemArquivo(String imagemArquivo) {
        this.imagemArquivo = imagemArquivo;
    }

    public String getImagemTipo() {
        return imagemTipo;
    }

    public void setImagemTipo(String imagemTipo) {
        this.imagemTipo = imagemTipo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }
}
