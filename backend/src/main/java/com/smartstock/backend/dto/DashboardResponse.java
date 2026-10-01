package com.smartstock.backend.dto;

import java.util.List;

import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.model.Pastilha;

public class DashboardResponse {

    private boolean podeVerPastilhas;
    private boolean podeVerFornecedores;
    private boolean podeVerMovimentacoes;

    private Long totalPastilhas;
    private Long totalFornecedores;
    private Long itensAbaixoDoMinimo;
    private List<Pastilha> alertas;
    private List<Movimentacao> ultimasMovimentacoes;

    public DashboardResponse(boolean podeVerPastilhas, boolean podeVerFornecedores, boolean podeVerMovimentacoes,
            Long totalPastilhas, Long totalFornecedores, Long itensAbaixoDoMinimo,
            List<Pastilha> alertas, List<Movimentacao> ultimasMovimentacoes) {
        this.podeVerPastilhas = podeVerPastilhas;
        this.podeVerFornecedores = podeVerFornecedores;
        this.podeVerMovimentacoes = podeVerMovimentacoes;
        this.totalPastilhas = totalPastilhas;
        this.totalFornecedores = totalFornecedores;
        this.itensAbaixoDoMinimo = itensAbaixoDoMinimo;
        this.alertas = alertas;
        this.ultimasMovimentacoes = ultimasMovimentacoes;
    }

    public boolean isPodeVerPastilhas() {
        return podeVerPastilhas;
    }

    public boolean isPodeVerFornecedores() {
        return podeVerFornecedores;
    }

    public boolean isPodeVerMovimentacoes() {
        return podeVerMovimentacoes;
    }

    public Long getTotalPastilhas() {
        return totalPastilhas;
    }

    public Long getTotalFornecedores() {
        return totalFornecedores;
    }

    public Long getItensAbaixoDoMinimo() {
        return itensAbaixoDoMinimo;
    }

    public List<Pastilha> getAlertas() {
        return alertas;
    }

    public List<Movimentacao> getUltimasMovimentacoes() {
        return ultimasMovimentacoes;
    }
}
