package com.smartstock.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.FornecedorProdutoItemRequest;
import com.smartstock.backend.dto.FornecedorProdutosRequest;
import com.smartstock.backend.dto.FornecedorRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.FornecedorProduto;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.repository.FornecedorProdutoRepository;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.PastilhaRepository;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;
    private final PastilhaRepository pastilhaRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository, PastilhaRepository pastilhaRepository) {
        this.fornecedorRepository = fornecedorRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.pastilhaRepository = pastilhaRepository;
    }

    public List<Fornecedor> listar() {
        return fornecedorRepository.findAll();
    }

    public Fornecedor buscarPorId(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + id));
    }

    public Fornecedor criar(FornecedorRequest request) {
        Fornecedor fornecedor = new Fornecedor();
        aplicar(fornecedor, request);
        return fornecedorRepository.save(fornecedor);
    }

    public Fornecedor atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarPorId(id);
        aplicar(fornecedor, request);
        return fornecedorRepository.save(fornecedor);
    }

    public List<FornecedorProduto> listarProdutos() {
        return fornecedorProdutoRepository.findAll();
    }

    /** Define quais peças o fornecedor entrega e o preço unitário de cada uma (substitui a lista anterior). */
    @Transactional
    public List<FornecedorProduto> salvarProdutos(Long fornecedorId, FornecedorProdutosRequest request) {
        Fornecedor fornecedor = buscarPorId(fornecedorId);

        Set<Long> vistos = new HashSet<>();
        for (FornecedorProdutoItemRequest item : request.getProdutos()) {
            if (!vistos.add(item.getPastilhaId())) {
                throw new BusinessException("A mesma peça foi informada mais de uma vez para este fornecedor.");
            }
        }

        // atualiza no lugar (em vez de apagar e recriar) para respeitar a restrição de unicidade
        Map<Long, FornecedorProduto> existentes = fornecedorProdutoRepository
                .findByFornecedorIdOrderByPastilhaCodigoAsc(fornecedorId).stream()
                .collect(Collectors.toMap(fp -> fp.getPastilha().getId(), Function.identity()));

        for (FornecedorProduto antigo : existentes.values()) {
            if (!vistos.contains(antigo.getPastilha().getId())) {
                fornecedorProdutoRepository.delete(antigo);
            }
        }
        for (FornecedorProdutoItemRequest item : request.getProdutos()) {
            FornecedorProduto fp = existentes.get(item.getPastilhaId());
            if (fp == null) {
                Pastilha pastilha = pastilhaRepository.findById(item.getPastilhaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Pastilha não encontrada: " + item.getPastilhaId()));
                fp = new FornecedorProduto();
                fp.setFornecedor(fornecedor);
                fp.setPastilha(pastilha);
            }
            fp.setPreco(item.getPreco());
            fornecedorProdutoRepository.save(fp);
        }
        return fornecedorProdutoRepository.findByFornecedorIdOrderByPastilhaCodigoAsc(fornecedorId);
    }

    @Transactional
    public void excluir(Long id) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedorProdutoRepository.deleteByFornecedorId(id);
        fornecedorRepository.delete(fornecedor);
    }

    private void aplicar(Fornecedor fornecedor, FornecedorRequest request) {
        fornecedor.setNome(request.getNome());
        fornecedor.setTipo(request.getTipo());
        fornecedor.setContato(request.getContato());
        fornecedor.setTelefone(request.getTelefone());
        fornecedor.setEmail(request.getEmail());
    }
}
