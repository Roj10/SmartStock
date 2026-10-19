package com.smartstock.backend.controller;

import java.util.List;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.dto.CalendarioUpdateRequest;
import com.smartstock.backend.dto.ChecklistToggleRequest;
import com.smartstock.backend.dto.EnviarPedidoRequest;
import com.smartstock.backend.dto.ProjetoRequest;
import com.smartstock.backend.dto.SenhaAdminRequest;
import com.smartstock.backend.dto.StatusProjetoRequest;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.service.AnexoProjetoService;
import com.smartstock.backend.service.AnexoProjetoService.ArquivoAnexo;
import com.smartstock.backend.service.ProjetoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projetos")
@PreAuthorize("hasAuthority('PERM_PROJETOS')")
public class ProjetoController {

    private final ProjetoService projetoService;
    private final AnexoProjetoService anexoService;

    public ProjetoController(ProjetoService projetoService, AnexoProjetoService anexoService) {
        this.projetoService = projetoService;
        this.anexoService = anexoService;
    }

    @GetMapping("/progresso")
    public List<Projeto> listarProgresso() {
        return projetoService.listarProgresso();
    }

    @GetMapping("/calendario")
    public List<Projeto> listarCalendario() {
        return projetoService.listarCalendario();
    }

    @GetMapping("/{id}")
    public Projeto buscar(@PathVariable Long id) {
        return projetoService.buscarPorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Projeto criar(@Valid @RequestBody ProjetoRequest request) {
        return projetoService.criar(request, List.of());
    }

    /** Novo projeto com arquivos anexados: "dados" (JSON do projeto) + "arquivos" (opcional, vários). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Projeto criarComAnexos(@Valid @RequestPart("dados") ProjetoRequest request,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos) {
        return projetoService.criar(request, arquivos);
    }

    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Projeto adicionarAnexos(@PathVariable Long id, @RequestPart("arquivos") List<MultipartFile> arquivos,
            @RequestParam(value = "senhaAdmin", required = false) String senhaAdmin) {
        return projetoService.adicionarAnexos(id, arquivos, senhaAdmin);
    }

    @DeleteMapping("/{id}/anexos/{anexoId}")
    public Projeto removerAnexo(@PathVariable Long id, @PathVariable Long anexoId,
            @RequestBody(required = false) SenhaAdminRequest request) {
        return projetoService.removerAnexo(id, anexoId, request != null ? request.getSenhaAdmin() : null);
    }

    /** Entrega o arquivo: PDF e imagens abrem no navegador; os demais (DWG, STEP, ZIP...) são baixados. */
    @GetMapping("/{id}/anexos/{anexoId}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Long id, @PathVariable Long anexoId) {
        ArquivoAnexo arquivo = anexoService.abrir(id, anexoId);
        ContentDisposition disposicao = (arquivo.abrirNoNavegador() ? ContentDisposition.inline()
                : ContentDisposition.attachment()).filename(arquivo.nome(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo.recurso());
    }

    @PutMapping("/{id}")
    public Projeto atualizar(@PathVariable Long id, @Valid @RequestBody ProjetoRequest request) {
        return projetoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public Projeto atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusProjetoRequest request) {
        return projetoService.atualizarStatus(id, request.getStatus());
    }

    @PostMapping("/{id}/enviar-pedido")
    public Projeto enviarPedido(@PathVariable Long id, @Valid @RequestBody EnviarPedidoRequest request) {
        return projetoService.enviarPedido(id, request);
    }

    @PostMapping("/{id}/entregar")
    public Projeto marcarComoEntregue(@PathVariable Long id) {
        return projetoService.marcarComoEntregue(id);
    }

    @PatchMapping("/{id}/calendario")
    public Projeto atualizarCalendario(@PathVariable Long id, @RequestBody CalendarioUpdateRequest request) {
        return projetoService.atualizarCalendario(id, request);
    }

    @PatchMapping("/{id}/checklist/{itemId}")
    public Projeto atualizarChecklistItem(@PathVariable Long id, @PathVariable Long itemId,
            @RequestBody ChecklistToggleRequest request) {
        return projetoService.atualizarChecklistItem(id, itemId, request.isConcluido());
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, @RequestBody(required = false) SenhaAdminRequest request) {
        projetoService.excluir(id, request != null ? request.getSenhaAdmin() : null);
    }
}
