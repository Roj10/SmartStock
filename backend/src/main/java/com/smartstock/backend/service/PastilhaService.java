package com.smartstock.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.PastilhaRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.PastilhaRepository;

@Service
public class PastilhaService {

    private final PastilhaRepository pastilhaRepository;
    private final FornecedorRepository fornecedorRepository;

    public PastilhaService(PastilhaRepository pastilhaRepository, FornecedorRepository fornecedorRepository) {
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    public List<Pastilha> listar() {
        return pastilhaRepository.findAll();
    }

    public Pastilha buscarPorId(Long id) {
        return pastilhaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pastilha não encontrada: " + id));
    }

    public List<Pastilha> listarAlertas() {
        return pastilhaRepository.findAlertasEstoqueMinimo();
    }

    public Pastilha criar(PastilhaRequest request) {
        if (pastilhaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe uma pastilha cadastrada com o código " + request.getCodigo());
        }

        Pastilha pastilha = new Pastilha();
        aplicar(pastilha, request);
        pastilha.setQuantidadeAtual(request.getQuantidadeAtual() != null ? request.getQuantidadeAtual() : 0);
        return pastilhaRepository.save(pastilha);
    }

    public Pastilha atualizar(Long id, PastilhaRequest request) {
        Pastilha pastilha = buscarPorId(id);

        if (!pastilha.getCodigo().equals(request.getCodigo()) && pastilhaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe uma pastilha cadastrada com o código " + request.getCodigo());
        }

        aplicar(pastilha, request);
        return pastilhaRepository.save(pastilha);
    }

    public void excluir(Long id) {
        Pastilha pastilha = buscarPorId(id);
        pastilhaRepository.delete(pastilha);
    }

    private void aplicar(Pastilha pastilha, PastilhaRequest request) {
        pastilha.setCodigo(request.getCodigo());
        pastilha.setDescricao(request.getDescricao());
        pastilha.setEstoqueMinimo(request.getEstoqueMinimo());

        if (request.getFabricanteId() != null) {
            Fornecedor fabricante = fornecedorRepository.findById(request.getFabricanteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fabricante não encontrado: " + request.getFabricanteId()));
            pastilha.setFabricante(fabricante);
        } else {
            pastilha.setFabricante(null);
        }
    }
}
