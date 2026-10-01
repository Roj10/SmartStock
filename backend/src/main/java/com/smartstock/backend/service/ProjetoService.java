package com.smartstock.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.CalendarioUpdateRequest;
import com.smartstock.backend.dto.ChecklistItemRequest;
import com.smartstock.backend.dto.ProjetoMaterialRequest;
import com.smartstock.backend.dto.ProjetoRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.ChecklistItem;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoMaterial;
import com.smartstock.backend.model.StatusProjeto;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.ProjetoRepository;

@Service
public class ProjetoService {

    private static final List<StatusProjeto> STATUS_PROGRESSO = List.of(StatusProjeto.AGUARDANDO, StatusProjeto.EM_PRODUCAO);
    private static final List<StatusProjeto> STATUS_CALENDARIO = List.of(StatusProjeto.FINALIZADO, StatusProjeto.ENTREGUE);

    private final ProjetoRepository projetoRepository;
    private final PastilhaRepository pastilhaRepository;

    public ProjetoService(ProjetoRepository projetoRepository, PastilhaRepository pastilhaRepository) {
        this.projetoRepository = projetoRepository;
        this.pastilhaRepository = pastilhaRepository;
    }

    public List<Projeto> listarProgresso() {
        return projetoRepository.findByStatusInOrderByDataCriacaoDesc(STATUS_PROGRESSO);
    }

    public List<Projeto> listarCalendario() {
        return projetoRepository.findByStatusInOrderByDataCriacaoDesc(STATUS_CALENDARIO);
    }

    public Projeto buscarPorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + id));
    }

    public Projeto criar(ProjetoRequest request) {
        Projeto projeto = new Projeto();
        projeto.setStatus(StatusProjeto.AGUARDANDO);
        projeto.setDataCriacao(LocalDateTime.now());
        aplicar(projeto, request);
        return projetoRepository.save(projeto);
    }

    public Projeto atualizar(Long id, ProjetoRequest request) {
        Projeto projeto = buscarPorId(id);
        aplicar(projeto, request);
        return projetoRepository.save(projeto);
    }

    public Projeto atualizarStatus(Long id, StatusProjeto novoStatus) {
        Projeto projeto = buscarPorId(id);

        if (novoStatus == StatusProjeto.ENTREGUE) {
            throw new BusinessException("Use a ação \"Marcar como entregue\" na aba Calendário para concluir o pedido.");
        }

        if (projeto.getStatus() == StatusProjeto.ENTREGUE) {
            throw new BusinessException("Este projeto já foi entregue e não pode voltar de status.");
        }

        if (novoStatus == StatusProjeto.EM_PRODUCAO && projeto.getDataInicioProducao() == null) {
            projeto.setDataInicioProducao(LocalDateTime.now());
        }
        if (novoStatus == StatusProjeto.FINALIZADO) {
            projeto.setDataFinalizacaoProducao(LocalDateTime.now());
        }

        projeto.setStatus(novoStatus);
        return projetoRepository.save(projeto);
    }

    public Projeto marcarComoEntregue(Long id) {
        Projeto projeto = buscarPorId(id);
        if (projeto.getStatus() != StatusProjeto.FINALIZADO) {
            throw new BusinessException("Apenas projetos finalizados podem ser marcados como entregues.");
        }
        projeto.setStatus(StatusProjeto.ENTREGUE);
        projeto.setDataEntrega(LocalDateTime.now());
        return projetoRepository.save(projeto);
    }

    public Projeto atualizarCalendario(Long id, CalendarioUpdateRequest request) {
        Projeto projeto = buscarPorId(id);
        projeto.setDataPedido(request.getDataPedido());
        projeto.setMetaEntrega(request.getMetaEntrega());
        return projetoRepository.save(projeto);
    }

    public Projeto atualizarChecklistItem(Long projetoId, Long itemId, boolean concluido) {
        Projeto projeto = buscarPorId(projetoId);
        ChecklistItem item = projeto.getChecklist().stream()
                .filter(c -> c.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de checklist não encontrado: " + itemId));
        item.setConcluido(concluido);
        return projetoRepository.save(projeto);
    }

    public void excluir(Long id) {
        Projeto projeto = buscarPorId(id);
        projetoRepository.delete(projeto);
    }

    private void aplicar(Projeto projeto, ProjetoRequest request) {
        projeto.setNome(request.getNome());
        projeto.setDescricao(request.getDescricao());

        projeto.getChecklist().clear();
        int ordem = 0;
        for (ChecklistItemRequest itemRequest : request.getChecklist()) {
            ChecklistItem item = new ChecklistItem();
            item.setProjeto(projeto);
            item.setTexto(itemRequest.getTexto());
            item.setConcluido(itemRequest.isConcluido());
            item.setOrdem(ordem++);
            projeto.getChecklist().add(item);
        }

        projeto.getMateriais().clear();
        List<ProjetoMaterial> materiais = new ArrayList<>();
        for (ProjetoMaterialRequest materialRequest : request.getMateriais()) {
            Pastilha pastilha = pastilhaRepository.findById(materialRequest.getPastilhaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pastilha não encontrada: " + materialRequest.getPastilhaId()));
            ProjetoMaterial material = new ProjetoMaterial();
            material.setProjeto(projeto);
            material.setPastilha(pastilha);
            material.setQuantidade(materialRequest.getQuantidade());
            materiais.add(material);
        }
        projeto.getMateriais().addAll(materiais);
    }
}
