package com.smartstock.backend.dto;

import java.util.List;

import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.model.Pastilha;

public class DashboardResponse {

    private long totalPastilhas;
    private long totalFornecedores;
    private long itensAbaixoDoMinimo;
    private List<Pastilha> alertas;
    private List<Movimentacao> ultimasMovimentacoes;

    public DashboardResponse(long totalPastilhas, long totalFornecedores, long itensAbaixoDoMinimo,
            List<Pastilha> alertas, List<Movimentacao> ultimasMovimentacoes) {
        this.totalPastilhas = totalPastilhas;
        this.totalFornecedores = totalFornecedores;
        this.itensAbaixoDoMinimo = itensAbaixoDoMinimo;
        this.alertas = alertas;
        this.ultimasMovimentacoes = ultimasMovimentacoes;
    }

    public long getTotalPastilhas() {
        return totalPastilhas;
    }

    public long getTotalFornecedores() {
        return totalFornecedores;
    }

    public long getItensAbaixoDoMinimo() {
        return itensAbaixoDoMinimo;
    }

    public List<Pastilha> getAlertas() {
        return alertas;
    }

    public List<Movimentacao> getUltimasMovimentacoes() {
        return ultimasMovimentacoes;
    }
}
