package com.smartstock.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.FinanceiroDtos.CustoMateriais;
import com.smartstock.backend.dto.FinanceiroDtos.VendaResposta;
import com.smartstock.backend.dto.VendaRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.StatusVenda;
import com.smartstock.backend.model.Venda;
import com.smartstock.backend.repository.ProjetoRepository;
import com.smartstock.backend.repository.VendaRepository;

@Service
@Transactional
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ProjetoRepository projetoRepository;
    private final CalculadoraCusto calculadoraCusto;

    public VendaService(VendaRepository vendaRepository, ProjetoRepository projetoRepository,
            CalculadoraCusto calculadoraCusto) {
        this.vendaRepository = vendaRepository;
        this.projetoRepository = projetoRepository;
        this.calculadoraCusto = calculadoraCusto;
    }

    @Transactional(readOnly = true)
    public List<VendaResposta> listar() {
        Map<Long, BigDecimal> precos = calculadoraCusto.menoresPrecos();
        return vendaRepository.findAllByOrderByDataCriacaoDescIdDesc().stream()
                .map(v -> paraResposta(v, precos))
                .toList();
    }

    public VendaResposta criar(VendaRequest request) {
        Venda venda = new Venda();
        venda.setStatus(StatusVenda.PLANO);
        venda.setDataCriacao(LocalDate.now());
        aplicar(venda, request);
        return resposta(vendaRepository.save(venda));
    }

    public VendaResposta atualizar(Long id, VendaRequest request) {
        Venda venda = buscar(id);
        aplicar(venda, request);
        return resposta(vendaRepository.save(venda));
    }

    /** Fecha o plano: ele passa a contar como venda realizada. */
    public VendaResposta fechar(Long id) {
        Venda venda = buscar(id);
        if (venda.getStatus() != StatusVenda.PLANO) {
            throw new BusinessException("Esta venda já foi fechada.");
        }
        venda.setStatus(StatusVenda.VENDA);
        venda.setDataVenda(LocalDate.now());
        return resposta(vendaRepository.save(venda));
    }

    public void excluir(Long id) {
        vendaRepository.delete(buscar(id));
    }

    private Venda buscar(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venda/plano não encontrado: " + id));
    }

    private void aplicar(Venda venda, VendaRequest request) {
        venda.setCliente(request.getCliente().trim());
        venda.setDescricao(request.getDescricao());
        venda.setValor(request.getValor());
        if (request.getProjetoId() != null) {
            Projeto projeto = projetoRepository.findById(request.getProjetoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + request.getProjetoId()));
            venda.setProjeto(projeto);
        } else {
            venda.setProjeto(null);
        }
    }

    private VendaResposta resposta(Venda venda) {
        return paraResposta(venda, calculadoraCusto.menoresPrecos());
    }

    public VendaResposta paraResposta(Venda venda, Map<Long, BigDecimal> precos) {
        Projeto projeto = venda.getProjeto();
        CustoMateriais custo = projeto != null ? calculadoraCusto.custoDoProjeto(projeto, precos) : null;
        BigDecimal margem = custo != null ? venda.getValor().subtract(custo.valor()) : null;
        return new VendaResposta(venda.getId(), venda.getCliente(),
                projeto != null ? projeto.getId() : null, projeto != null ? projeto.getNome() : null,
                venda.getDescricao(), venda.getValor(), venda.getStatus(), venda.getDataCriacao(),
                venda.getDataVenda(), custo != null ? custo.valor() : null,
                custo != null && custo.incompleto(), margem);
    }
}
