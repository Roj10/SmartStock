package com.smartstock.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.FornecedorRequest;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.repository.FornecedorRepository;

@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
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

    public void excluir(Long id) {
        Fornecedor fornecedor = buscarPorId(id);
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
