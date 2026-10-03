package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.CalendarioUpdateRequest;
import com.smartstock.backend.dto.ChecklistToggleRequest;
import com.smartstock.backend.dto.EnviarPedidoRequest;
import com.smartstock.backend.dto.ProjetoRequest;
import com.smartstock.backend.dto.StatusProjetoRequest;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.service.ProjetoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projetos")
@PreAuthorize("hasAuthority('PERM_PROJETOS')")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
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

    @PostMapping
    public Projeto criar(@Valid @RequestBody ProjetoRequest request) {
        return projetoService.criar(request);
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
    public void excluir(@PathVariable Long id) {
        projetoService.excluir(id);
    }
}
