package com.smartstock.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.EntradaLoteRequest;
import com.smartstock.backend.dto.MovimentacaoRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoMaterial;
import com.smartstock.backend.model.TipoMovimentacao;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.MovimentacaoRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.UsuarioRepository;

/**
 * <b>Em palavras simples:</b> Regras de entradas e saídas de estoque.
 *
 * Entrada: soma a quantidade ao item. Saída: confere se há quantidade suficiente e subtrai. Toda
 * movimentação fica registrada com data, quem fez e (na entrada) o fornecedor. Na entrada em lote, se
 * uma linha der erro, nenhuma é gravada (@Transactional = "tudo ou nada").
 */
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

    /**
     * Entrada de estoque (ex.: chegou uma compra): soma a quantidade ao item e registra quem fez.
     * {@code @Transactional} = "tudo ou nada": a nova quantidade e o registro da movimentação são gravados
     * juntos; se um falhar, nenhum fica gravado.
     */
    @Transactional
    public Movimentacao registrarEntrada(MovimentacaoRequest request) {
        Pastilha pastilha = buscarPastilha(request.getPastilhaId());

        Movimentacao movimentacao = criarMovimentacao(request, pastilha, TipoMovimentacao.ENTRADA);
        pastilha.setQuantidadeAtual(pastilha.getQuantidadeAtual() + request.getQuantidade());

        pastilhaRepository.save(pastilha);
        return movimentacaoRepository.save(movimentacao);
    }

    /** Registra várias entradas de uma vez; se alguma falhar, nenhuma é gravada. */
    @Transactional
    public List<Movimentacao> registrarEntradaLote(EntradaLoteRequest request) {
        List<Movimentacao> registradas = new ArrayList<>();
        for (MovimentacaoRequest item : request.getItens()) {
            if (item.getObservacao() == null || item.getObservacao().isBlank()) {
                item.setObservacao(request.getObservacao());
            }
            registradas.add(registrarEntrada(item));
        }
        return registradas;
    }

    /**
     * Baixa do estoque exatamente os materiais solicitados no projeto, quando o
     * pedido é enviado ao cliente. Se faltar estoque de qualquer item, nada é baixado.
     */
    @Transactional
    public List<Movimentacao> registrarSaidasDoProjeto(Projeto projeto, String cliente) {
        // 1) soma o total necessário de cada item (o mesmo item pode aparecer em mais de uma linha)
        Map<Long, Integer> necessario = new LinkedHashMap<>();
        for (ProjetoMaterial material : projeto.getMateriais()) {
            necessario.merge(material.getPastilha().getId(), material.getQuantidade(), Integer::sum);
        }

        // 2) confere TUDO antes de baixar qualquer coisa, e junta todas as faltas numa mensagem só
        List<String> faltas = new ArrayList<>();
        for (Map.Entry<Long, Integer> item : necessario.entrySet()) {
            Pastilha pastilha = buscarPastilha(item.getKey());
            if (pastilha.getQuantidadeAtual() < item.getValue()) {
                faltas.add(pastilha.getCodigo() + " (necessário " + item.getValue()
                        + ", disponível " + pastilha.getQuantidadeAtual() + ")");
            }
        }
        if (!faltas.isEmpty()) {
            throw new BusinessException("Estoque insuficiente para enviar o pedido: " + String.join("; ", faltas)
                    + ". Registre uma entrada de estoque e tente novamente.");
        }

        // 3) com tudo disponível, registra uma saída por item, identificando o projeto e o cliente
        String observacao = String.format("Saída automática do projeto OP-%03d - %s (cliente: %s)",
                projeto.getId(), projeto.getNome(), cliente);
        List<Movimentacao> registradas = new ArrayList<>();
        for (Map.Entry<Long, Integer> item : necessario.entrySet()) {
            MovimentacaoRequest saida = new MovimentacaoRequest();
            saida.setPastilhaId(item.getKey());
            saida.setQuantidade(item.getValue());
            saida.setObservacao(observacao);
            registradas.add(registrarSaida(saida));
        }
        return registradas;
    }

    /** Saída de estoque (uso na produção, manutenção...): recusada se pedir mais do que existe. */
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

    /** Descobre quem está logado (pelo token do pedido) para gravar como responsável pela movimentação. */
    private java.util.Optional<Usuario> usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return usuarioRepository.findByUsername(authentication.getName());
    }
}
