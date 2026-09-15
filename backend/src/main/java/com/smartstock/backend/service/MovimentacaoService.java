package com.smartstock.backend.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.MovimentacaoRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.TipoMovimentacao;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.MovimentacaoRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.UsuarioRepository;

@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final PastilhaRepository pastilhaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository, PastilhaRepository pastilhaRepository,
            FornecedorRepository fornecedorRepository, UsuarioRepository usuarioRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Movimentacao> listar() {
        return movimentacaoRepository.findAllByOrderByDataHoraDesc();
    }

    public List<Movimentacao> listarPorPastilha(Long pastilhaId) {
        return movimentacaoRepository.findByPastilhaIdOrderByDataHoraDesc(pastilhaId);
    }

    @Transactional
    public Movimentacao registrarEntrada(MovimentacaoRequest request) {
        Pastilha pastilha = buscarPastilha(request.getPastilhaId());

        Movimentacao movimentacao = criarMovimentacao(request, pastilha, TipoMovimentacao.ENTRADA);
        pastilha.setQuantidadeAtual(pastilha.getQuantidadeAtual() + request.getQuantidade());

        pastilhaRepository.save(pastilha);
        return movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public Movimentacao registrarSaida(MovimentacaoRequest request) {
        Pastilha pastilha = buscarPastilha(request.getPastilhaId());

        if (pastilha.getQuantidadeAtual() < request.getQuantidade()) {
            throw new BusinessException("Estoque insuficiente para a pastilha " + pastilha.getCodigo()
                    + ". Disponível: " + pastilha.getQuantidadeAtual());
        }

        Movimentacao movimentacao = criarMovimentacao(request, pastilha, TipoMovimentacao.SAIDA);
        pastilha.setQuantidadeAtual(pastilha.getQuantidadeAtual() - request.getQuantidade());

        pastilhaRepository.save(pastilha);
        return movimentacaoRepository.save(movimentacao);
    }

    private Movimentacao criarMovimentacao(MovimentacaoRequest request, Pastilha pastilha, TipoMovimentacao tipo) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setPastilha(pastilha);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setObservacao(request.getObservacao());

        if (request.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository.findById(request.getFornecedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + request.getFornecedorId()));
            movimentacao.setFornecedor(fornecedor);
        }

        usuarioAutenticado().ifPresent(movimentacao::setUsuario);

        return movimentacao;
    }

    private Pastilha buscarPastilha(Long id) {
        return pastilhaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pastilha não encontrada: " + id));
    }

    private java.util.Optional<Usuario> usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return usuarioRepository.findByUsername(authentication.getName());
    }
}
