package com.smartstock.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.smartstock.backend.model.StatusProjeto;
import com.smartstock.backend.model.StatusVenda;

/** Respostas do módulo Financeiro. */
public final class FinanceiroDtos {

    private FinanceiroDtos() {
    }

    public record FornecedorPreco(Long fornecedorId, String fornecedor, BigDecimal preco) {
    }

    public record PecaFinanceira(Long id, String codigo, String descricao, String imagemUrl, Integer quantidadeAtual,
            List<FornecedorPreco> fornecedores, BigDecimal menorPreco, BigDecimal valorEmEstoque) {
    }

    public record CustoMateriais(BigDecimal valor, boolean incompleto) {
    }

    public record ProjetoFinanceiro(Long id, String nome, String cliente, StatusProjeto status,
            BigDecimal custoMateriais, boolean custoIncompleto, BigDecimal valorPlano, BigDecimal valorVenda,
            BigDecimal margem) {
    }

    public record VendaResposta(Long id, String cliente, Long projetoId, String projetoNome, String descricao,
            BigDecimal valor, StatusVenda status, LocalDate dataCriacao, LocalDate dataVenda,
            BigDecimal custoMateriais, boolean custoIncompleto, BigDecimal margem) {
    }

    public record Resumo(BigDecimal valorEmEstoque, BigDecimal totalVendas, long quantidadeVendas,
            BigDecimal totalPlanos, long quantidadePlanos, BigDecimal margemVendas) {
    }
}
