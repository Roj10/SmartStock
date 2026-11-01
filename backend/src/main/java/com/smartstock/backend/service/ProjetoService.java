package com.smartstock.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.smartstock.backend.repository.VendaRepository;

/**
 * <b>Em palavras simples:</b> Regras dos projetos (ordens de produção), o coração da aba Progresso.
 *
 * - Criar: com checklist, materiais e arquivos; começa em AGUARDANDO.
 * - Etapas: anda uma por vez, para frente ou para trás (AGUARDANDO ↔ EM_PRODUCAO ↔ EM_ESTOQUE ↔ PRONTO_ENTREGA).
 * - Checklist: só pode ser marcado durante a produção.
 * - Enviar pedido: dá baixa em TODOS os materiais de uma vez; se faltar um, nada é baixado.
 * - Editar, excluir e mexer nos arquivos: só com a senha de um administrador.
 */
@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final PastilhaRepository pastilhaRepository;
    private final MovimentacaoService movimentacaoService;
    private final VendaRepository vendaRepository;
    private final SenhaAdminService senhaAdminService;
    private final AnexoProjetoService anexoService;

    // "Injeção de dependências": o Spring entrega prontas, pelo construtor, as outras peças que este
    // serviço usa (acesso ao banco, movimentações, senha do administrador, arquivos). Ninguém precisa criá-las.
    public ProjetoService(ProjetoRepository projetoRepository, PastilhaRepository pastilhaRepository,
            MovimentacaoService movimentacaoService, VendaRepository vendaRepository,
            SenhaAdminService senhaAdminService, AnexoProjetoService anexoService) {
        this.projetoRepository = projetoRepository;
        this.pastilhaRepository = pastilhaRepository;
        this.movimentacaoService = movimentacaoService;
        this.vendaRepository = vendaRepository;
        this.senhaAdminService = senhaAdminService;
        this.anexoService = anexoService;
    }

    /** Projetos ainda em produção (aba Progresso), do mais antigo para o mais novo: quem chegou antes é feito antes. */
    public List<Projeto> listarProgresso() {
        return projetoRepository.findByStatusInOrderByDataCriacaoAscIdAsc(StatusProjeto.ETAPAS_PROGRESSO);
    }

    /** Pedidos já enviados ao cliente e entregues: o que aparece na aba Calendário. */
    public List<Projeto> listarCalendario() {
        return projetoRepository.findByStatusInOrderByDataCriacaoDesc(StatusProjeto.ETAPAS_CALENDARIO);
    }

    public Projeto buscarPorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + id));
    }

    /**
     * Cria o projeto já com os arquivos anexados (novos e/ou copiados do projeto repetido).
     * Se algum arquivo for recusado, o projeto não é criado.
     */
    @Transactional
    public Projeto criar(ProjetoRequest request, List<MultipartFile> arquivos) {
        Projeto projeto = new Projeto();
        projeto.setStatus(StatusProjeto.AGUARDANDO);
        projeto.setDataCriacao(LocalDateTime.now());
        // confere os arquivos (tipo e tamanho) ANTES de gravar qualquer coisa
        anexoService.validar(projeto, arquivos);
        aplicar(projeto, request);
        projeto = projetoRepository.save(projeto);
        try {
            anexoService.copiar(projeto, request.getAnexosCopiar());
            anexoService.anexar(projeto, arquivos);
        } catch (RuntimeException e) {
            // se o envio de algum arquivo falhar, apaga os que já subiram para o Storage; o banco é
            // desfeito sozinho pelo @Transactional, e assim não sobra "meio projeto"
            anexoService.apagarArquivosDoProjeto(projeto);
            throw e;
        }
        return projeto;
    }

    /** Anexar arquivos a um projeto que já existe é uma edição: exige a senha de um administrador. */
    @Transactional
    public Projeto adicionarAnexos(Long id, List<MultipartFile> arquivos, String senhaAdmin) {
        Projeto projeto = buscarPorId(id);
        senhaAdminService.exigir(senhaAdmin, "anexar arquivos ao projeto");
        anexoService.anexar(projeto, arquivos);
        return projeto;
    }

    /** Remover um arquivo do projeto também exige a senha de um administrador. */
    @Transactional
    public Projeto removerAnexo(Long id, Long anexoId, String senhaAdmin) {
        Projeto projeto = buscarPorId(id);
        senhaAdminService.exigir(senhaAdmin, "remover arquivos do projeto");
        anexoService.remover(projeto, anexoId);
        return projetoRepository.save(projeto);
    }

    /** Editar um projeto existente exige a senha de um administrador. */
    public Projeto atualizar(Long id, ProjetoRequest request) {
        Projeto projeto = buscarPorId(id);
        senhaAdminService.exigir(request.getSenhaAdmin(), "editar o projeto");
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
        // "ordinal" é a posição da etapa na lista; a diferença tem de ser 1 (vizinha), para frente ou para trás
        if (Math.abs(novoStatus.ordinal() - atual.ordinal()) != 1) {
            throw new BusinessException("A ordem de produção deve ser seguida etapa por etapa.");
        }

        // guarda quando a produção começou e terminou (usado para acompanhar prazos)
        if (novoStatus == StatusProjeto.EM_PRODUCAO && projeto.getDataInicioProducao() == null) {
            projeto.setDataInicioProducao(LocalDateTime.now());
        }
        if (novoStatus == StatusProjeto.EM_ESTOQUE) {
            projeto.setDataFinalizacaoProducao(LocalDateTime.now());
        }

        projeto.setStatus(novoStatus);
        return projetoRepository.save(projeto);
    }

    /**
     * Envia o pedido ao cliente: registra cliente e meta de entrega, dá baixa no estoque de todos os
     * materiais do projeto e o move para o Calendário. Se faltar algum material, nada muda (a baixa e a
     * mudança de etapa estão na mesma transação "tudo ou nada").
     */
    @Transactional
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
        movimentacaoService.registrarSaidasDoProjeto(projeto, projeto.getCliente());

        projeto.setStatus(StatusProjeto.PEDIDO_ENVIADO);
        return projetoRepository.save(projeto);
    }

    /** Última etapa: o cliente recebeu. Guarda a data da entrega (o pedido fica no histórico). */
    public Projeto marcarComoEntregue(Long id) {
        Projeto projeto = buscarPorId(id);
        if (projeto.getStatus() != StatusProjeto.PEDIDO_ENVIADO) {
            throw new BusinessException("Apenas pedidos enviados podem ser marcados como entregues.");
        }
        projeto.setStatus(StatusProjeto.ENTREGUE);
        projeto.setDataEntrega(LocalDateTime.now());
        return projetoRepository.save(projeto);
    }

    /** Ajusta a data do pedido e a meta de entrega no Calendário (não precisa de senha). */
    public Projeto atualizarCalendario(Long id, CalendarioUpdateRequest request) {
        Projeto projeto = buscarPorId(id);
        projeto.setDataPedido(request.getDataPedido());
        projeto.setMetaEntrega(request.getMetaEntrega());
        return projetoRepository.save(projeto);
    }

    /** Marca ou desmarca uma etapa do checklist; só é permitido com o projeto Em produção. */
    public Projeto atualizarChecklistItem(Long projetoId, Long itemId, boolean concluido) {
        Projeto projeto = buscarPorId(projetoId);
        if (projeto.getStatus() != StatusProjeto.EM_PRODUCAO) {
            throw new BusinessException("As etapas do checklist só podem ser marcadas com o projeto Em produção.");
        }
        ChecklistItem item = projeto.getChecklist().stream()
                .filter(c -> c.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de checklist não encontrado: " + itemId));
        item.setConcluido(concluido);
        return projetoRepository.save(projeto);
    }

    /** Excluir um projeto exige a senha de um administrador. */
    @Transactional
    public void excluir(Long id, String senhaAdmin) {
        Projeto projeto = buscarPorId(id);
        senhaAdminService.exigir(senhaAdmin, "excluir o projeto");
        // vendas e planos já registrados continuam existindo, apenas sem o vínculo com o projeto
        vendaRepository.findByProjetoId(id).forEach(v -> v.setProjeto(null));
        anexoService.apagarArquivosDoProjeto(projeto);
        projetoRepository.delete(projeto);
    }

    /**
     * Copia os dados do formulário para o projeto (usado ao criar e ao editar). O checklist e os materiais
     * são montados de novo a partir da lista enviada.
     */
    private void aplicar(Projeto projeto, ProjetoRequest request) {
        projeto.setNome(request.getNome());
        projeto.setDescricao(request.getDescricao());
        projeto.setCliente(request.getCliente());

        // as etapas só podem ser marcadas enquanto o projeto está Em produção; nas demais colunas o estado atual é mantido
        boolean podeMarcar = projeto.getStatus() == StatusProjeto.EM_PRODUCAO;
        List<Boolean> concluidosAtuais = projeto.getChecklist().stream().map(ChecklistItem::isConcluido).toList();

        projeto.getChecklist().clear();
        int ordem = 0;
        for (ChecklistItemRequest itemRequest : request.getChecklist()) {
            ChecklistItem item = new ChecklistItem();
            item.setProjeto(projeto);
            item.setTexto(itemRequest.getTexto());
            item.setConcluido(podeMarcar
                    ? itemRequest.isConcluido()
                    : ordem < concluidosAtuais.size() && concluidosAtuais.get(ordem));
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
