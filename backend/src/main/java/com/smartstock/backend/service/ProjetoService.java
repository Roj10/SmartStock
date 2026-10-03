package com.smartstock.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.CalendarioUpdateRequest;
import com.smartstock.backend.dto.ChecklistItemRequest;
import com.smartstock.backend.dto.EnviarPedidoRequest;
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

    private final ProjetoRepository projetoRepository;
    private final PastilhaRepository pastilhaRepository;

    public ProjetoService(ProjetoRepository projetoRepository, PastilhaRepository pastilhaRepository) {
        this.projetoRepository = projetoRepository;
        this.pastilhaRepository = pastilhaRepository;
    }

    public List<Projeto> listarProgresso() {
        return projetoRepository.findByStatusInOrderByDataCriacaoAscIdAsc(StatusProjeto.ETAPAS_PROGRESSO);
    }

    public List<Projeto> listarCalendario() {
        return projetoRepository.findByStatusInOrderByDataCriacaoDesc(StatusProjeto.ETAPAS_CALENDARIO);
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

    /**
     * Move o projeto uma etapa para frente ou para trás na ordem de produção:
     * Aguardando início → Em produção → Em estoque → Pronto para entrega.
     * O envio do pedido (e a entrega) têm ações próprias.
     */
    public Projeto atualizarStatus(Long id, StatusProjeto novoStatus) {
        Projeto projeto = buscarPorId(id);
        StatusProjeto atual = projeto.getStatus();

        if (!atual.estaNoProgresso()) {
            throw new BusinessException("Este pedido já foi enviado e não volta para o quadro de produção.");
        }
        if (novoStatus == StatusProjeto.PEDIDO_ENVIADO) {
            throw new BusinessException("Use a ação \"Enviar pedido\" na coluna Pronto para entrega.");
        }
        if (novoStatus == StatusProjeto.ENTREGUE) {
            throw new BusinessException("Use a ação \"Marcar como entregue\" na aba Calendário para concluir o pedido.");
        }
        if (Math.abs(novoStatus.ordinal() - atual.ordinal()) != 1) {
            throw new BusinessException("A ordem de produção deve ser seguida etapa por etapa.");
        }

        if (novoStatus == StatusProjeto.EM_PRODUCAO && projeto.getDataInicioProducao() == null) {
            projeto.setDataInicioProducao(LocalDateTime.now());
        }
        if (novoStatus == StatusProjeto.EM_ESTOQUE) {
            projeto.setDataFinalizacaoProducao(LocalDateTime.now());
        }

        projeto.setStatus(novoStatus);
        return projetoRepository.save(projeto);
    }

    public Projeto enviarPedido(Long id, EnviarPedidoRequest request) {
        Projeto projeto = buscarPorId(id);
        if (projeto.getStatus() != StatusProjeto.PRONTO_ENTREGA) {
            throw new BusinessException("Apenas projetos na coluna Pronto para entrega podem ter o pedido enviado.");
        }
        projeto.setCliente(request.getCliente().trim());
        projeto.setDataPedido(LocalDate.now());
        if (request.getMetaEntrega() != null) {
            projeto.setMetaEntrega(request.getMetaEntrega());
        }
        projeto.setStatus(StatusProjeto.PEDIDO_ENVIADO);
        return projetoRepository.save(projeto);
    }

    public Projeto marcarComoEntregue(Long id) {
        Projeto projeto = buscarPorId(id);
        if (projeto.getStatus() != StatusProjeto.PEDIDO_ENVIADO) {
            throw new BusinessException("Apenas pedidos enviados podem ser marcados como entregues.");
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
        projeto.setCliente(request.getCliente());

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
