package com.smartstock.backend.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.smartstock.backend.dto.FinanceiroDtos.CustoMateriais;
import com.smartstock.backend.model.FornecedorProduto;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoMaterial;
import com.smartstock.backend.repository.FornecedorProdutoRepository;

/** Preço de referência de cada peça (o menor praticado pelos fornecedores) e custo de materiais dos projetos. */
@Component
public class CalculadoraCusto {

    private final FornecedorProdutoRepository fornecedorProdutoRepository;

    public CalculadoraCusto(FornecedorProdutoRepository fornecedorProdutoRepository) {
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
    }

    public Map<Long, BigDecimal> menoresPrecos() {
        Map<Long, BigDecimal> menores = new HashMap<>();
        for (FornecedorProduto fp : fornecedorProdutoRepository.findAll()) {
            menores.merge(fp.getPastilha().getId(), fp.getPreco(), BigDecimal::min);
        }
        return menores;
    }

    public CustoMateriais custoDoProjeto(Projeto projeto, Map<Long, BigDecimal> menoresPrecos) {
        BigDecimal total = BigDecimal.ZERO;
        boolean incompleto = false;
        for (ProjetoMaterial material : projeto.getMateriais()) {
            BigDecimal preco = menoresPrecos.get(material.getPastilha().getId());
            if (preco == null) {
                incompleto = true;
            } else {
                total = total.add(preco.multiply(BigDecimal.valueOf(material.getQuantidade())));
            }
        }
        return new CustoMateriais(total, incompleto);
    }
}
