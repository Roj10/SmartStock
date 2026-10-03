package com.smartstock.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.FinanceiroDtos.CustoMateriais;
import com.smartstock.backend.dto.FinanceiroDtos.FornecedorPreco;
import com.smartstock.backend.dto.FinanceiroDtos.PecaFinanceira;
import com.smartstock.backend.dto.FinanceiroDtos.ProjetoFinanceiro;
import com.smartstock.backend.dto.FinanceiroDtos.Resumo;
import com.smartstock.backend.dto.FinanceiroDtos.VendaResposta;
import com.smartstock.backend.model.FornecedorProduto;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.StatusVenda;
import com.smartstock.backend.model.Venda;
import com.smartstock.backend.repository.FornecedorProdutoRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.ProjetoRepository;
import com.smartstock.backend.repository.VendaRepository;

@Service
@Transactional(readOnly = true)
public class FinanceiroService {

    private final PastilhaRepository pastilhaRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;
    private final ProjetoRepository projetoRepository;
    private final VendaRepository vendaRepository;
    private final CalculadoraCusto calculadoraCusto;
    private final VendaService vendaService;

    public FinanceiroService(PastilhaRepository pastilhaRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository, ProjetoRepository projetoRepository,
            VendaRepository vendaRepository, CalculadoraCusto calculadoraCusto, VendaService vendaService) {
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.projetoRepository = projetoRepository;
        this.vendaRepository = vendaRepository;
        this.calculadoraCusto = calculadoraCusto;
        this.vendaService = vendaService;
    }

    /** Cada peça com o preço de cada fornecedor, o menor preço e o valor parado em estoque. */
    public List<PecaFinanceira> pecas() {
        Map<Long, List<FornecedorProduto>> porPastilha = fornecedorProdutoRepository.findAll().stream()
                .collect(Collectors.groupingBy(fp -> fp.getPastilha().getId()));

        List<PecaFinanceira> pecas = new ArrayList<>();
        for (Pastilha p : pastilhaRepository.findAll()) {
            List<FornecedorPreco> fornecedores = porPastilha.getOrDefault(p.getId(), List.of()).stream()
                    .map(fp -> new FornecedorPreco(fp.getFornecedor().getId(), fp.getFornecedor().getNome(), fp.getPreco()))
                    .sorted(Comparator.comparing(FornecedorPreco::preco))
                    .toList();
            BigDecimal menor = fornecedores.isEmpty() ? null : fornecedores.get(0).preco();
            BigDecimal valorEmEstoque = menor != null
                    ? menor.multiply(BigDecimal.valueOf(p.getQuantidadeAtual())) : null;
            pecas.add(new PecaFinanceira(p.getId(), p.getCodigo(), p.getDescricao(), p.getImagemUrl(),
                    p.getQuantidadeAtual(), fornecedores, menor, valorEmEstoque));
        }
        return pecas;
    }

    /** Custo de materiais de cada projeto frente ao que foi planejado/vendido para o cliente. */
    public List<ProjetoFinanceiro> projetos() {
        Map<Long, BigDecimal> precos = calculadoraCusto.menoresPrecos();
        Map<Long, List<Venda>> vendasPorProjeto = vendaRepository.findAll().stream()
                .filter(v -> v.getProjeto() != null)
                .collect(Collectors.groupingBy(v -> v.getProjeto().getId()));

        List<ProjetoFinanceiro> lista = new ArrayList<>();
        for (Projeto p : projetoRepository.findAll()) {
            List<Venda> vendas = vendasPorProjeto.getOrDefault(p.getId(), List.of());
            BigDecimal valorPlano = somar(vendas, StatusVenda.PLANO);
            BigDecimal valorVenda = somar(vendas, StatusVenda.VENDA);
            CustoMateriais custo = calculadoraCusto.custoDoProjeto(p, precos);
            BigDecimal referencia = valorVenda.signum() > 0 ? valorVenda : valorPlano;
            BigDecimal margem = referencia.signum() > 0 ? referencia.subtract(custo.valor()) : null;
            lista.add(new ProjetoFinanceiro(p.getId(), p.getNome(), p.getCliente(), p.getStatus(),
                    custo.valor(), custo.incompleto(), valorPlano, valorVenda, margem));
        }
        lista.sort(Comparator.comparing(ProjetoFinanceiro::id));
        return lista;
    }

    public Resumo resumo() {
        BigDecimal valorEstoque = pecas().stream()
                .map(PecaFinanceira::valorEmEstoque)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VendaResposta> vendas = vendaService.listar();
        BigDecimal totalVendas = BigDecimal.ZERO;
        BigDecimal totalPlanos = BigDecimal.ZERO;
        BigDecimal margemVendas = BigDecimal.ZERO;
        long qtdVendas = 0;
        long qtdPlanos = 0;
        for (VendaResposta v : vendas) {
            if (v.status() == StatusVenda.VENDA) {
                totalVendas = totalVendas.add(v.valor());
                qtdVendas++;
                if (v.margem() != null) {
                    margemVendas = margemVendas.add(v.margem());
                }
            } else {
                totalPlanos = totalPlanos.add(v.valor());
                qtdPlanos++;
            }
        }
        return new Resumo(valorEstoque, totalVendas, qtdVendas, totalPlanos, qtdPlanos, margemVendas);
    }

    private BigDecimal somar(List<Venda> vendas, StatusVenda status) {
        return vendas.stream().filter(v -> v.getStatus() == status)
                .map(Venda::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
