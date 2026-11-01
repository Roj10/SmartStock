package com.smartstock.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.ModeloProjetoDtos.MaterialModelo;
import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloRequest;
import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloResposta;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.ModeloProjeto;
import com.smartstock.backend.repository.ModeloProjetoRepository;
import com.smartstock.backend.repository.PastilhaRepository;

/**
 * <b>Em palavras simples:</b> Regras dos Modelos de projeto: salvar (pelo nome, sem duplicar), editar, excluir e aplicar um modelo
 * na criação de um projeto (copiando checklist e materiais).
 */
@Service
@Transactional
public class ModeloProjetoService {

    private final ModeloProjetoRepository modeloRepository;
    private final PastilhaRepository pastilhaRepository;

    public ModeloProjetoService(ModeloProjetoRepository modeloRepository, PastilhaRepository pastilhaRepository) {
        this.modeloRepository = modeloRepository;
        this.pastilhaRepository = pastilhaRepository;
    }

    @Transactional(readOnly = true)
    public List<ModeloResposta> listar() {
        return modeloRepository.findAllByOrderByNomeAsc().stream().map(this::resposta).toList();
    }

    /**
     * Salva um modelo. Se já existir um modelo com o mesmo nome (sem diferenciar maiúsculas), ele é atualizado:
     * assim, salvar de novo o "mesmo" projeto como modelo não gera duplicatas nem erro.
     */
    public ModeloResposta salvar(ModeloRequest request) {
        String nome = request.nome().trim();
        ModeloProjeto modelo = modeloRepository.findFirstByNomeIgnoreCase(nome).orElseGet(ModeloProjeto::new);
        return resposta(modeloRepository.save(aplicar(modelo, request)));
    }

    /** Um modelo pelo número. */
    @Transactional(readOnly = true)
    public ModeloResposta obter(Long id) {
        return resposta(buscar(id));
    }

    /** Edita um modelo existente (inclusive o nome, desde que não seja o nome de outro modelo). */
    public ModeloResposta atualizar(Long id, ModeloRequest request) {
        ModeloProjeto modelo = buscar(id);
        String nome = request.nome().trim();
        modeloRepository.findFirstByNomeIgnoreCase(nome).filter(outro -> !outro.getId().equals(id)).ifPresent(outro -> {
            throw new BusinessException("Já existe outro modelo chamado \"" + nome + "\".");
        });
        return resposta(modeloRepository.save(aplicar(modelo, request)));
    }

    private ModeloProjeto buscar(Long id) {
        return modeloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo não encontrado: " + id));
    }

    /** Copia nome, descrição, etapas e materiais do pedido para o modelo. */
    private ModeloProjeto aplicar(ModeloProjeto modelo, ModeloRequest request) {
        String nome = request.nome().trim();
        modelo.setNome(nome);
        modelo.setDescricao(request.descricao() == null || request.descricao().isBlank() ? null : request.descricao().trim());

        modelo.getChecklist().clear();
        if (request.checklist() != null) {
            request.checklist().stream().map(String::trim).filter(t -> !t.isEmpty()).forEach(modelo.getChecklist()::add);
        }

        modelo.getMateriais().clear();
        if (request.materiais() != null) {
            for (MaterialModelo m : request.materiais()) {
                if (!pastilhaRepository.existsById(m.pastilhaId())) {
                    throw new ResourceNotFoundException("Pastilha não encontrada: " + m.pastilhaId());
                }
                modelo.getMateriais().add(new ModeloProjeto.Material(m.pastilhaId(), m.quantidade()));
            }
        }
        return modelo;
    }

    public void excluir(Long id) {
        ModeloProjeto modelo = modeloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo não encontrado: " + id));
        modeloRepository.delete(modelo);
    }

    /** Quando uma pastilha é excluída, ela sai também dos modelos que a usavam. */
    public void removerPastilhaDosModelos(Long pastilhaId) {
        for (ModeloProjeto modelo : modeloRepository.findAll()) {
            modelo.getMateriais().removeIf(m -> m.getPastilhaId().equals(pastilhaId));
        }
    }

    private ModeloResposta resposta(ModeloProjeto m) {
        return new ModeloResposta(m.getId(), m.getNome(), m.getDescricao(), List.copyOf(m.getChecklist()),
                m.getMateriais().stream().map(x -> new MaterialModelo(x.getPastilhaId(), x.getQuantidade())).toList(),
                m.getDataCriacao());
    }
}
