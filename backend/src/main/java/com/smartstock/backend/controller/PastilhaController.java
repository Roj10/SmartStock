package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.dto.PastilhaRequest;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.service.PastilhaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pastilhas")
@PreAuthorize("hasAuthority('PERM_PASTILHAS') or hasAuthority('PERM_MOVIMENTACOES')")
public class PastilhaController {

    private final PastilhaService pastilhaService;

    public PastilhaController(PastilhaService pastilhaService) {
        this.pastilhaService = pastilhaService;
    }

    @GetMapping
    public List<Pastilha> listar() {
        return pastilhaService.listar();
    }

    @GetMapping("/alertas")
    public List<Pastilha> listarAlertas() {
        return pastilhaService.listarAlertas();
    }

    @GetMapping("/{id}")
    public Pastilha buscar(@PathVariable Long id) {
        return pastilhaService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PASTILHAS')")
    public Pastilha criar(@Valid @RequestBody PastilhaRequest request) {
        return pastilhaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PASTILHAS')")
    public Pastilha atualizar(@PathVariable Long id, @Valid @RequestBody PastilhaRequest request) {
        return pastilhaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PASTILHAS')")
    public void excluir(@PathVariable Long id) {
        pastilhaService.excluir(id);
    }

    @PostMapping("/{id}/imagem")
    @PreAuthorize("hasAuthority('PERM_PASTILHAS')")
    public Pastilha enviarImagem(@PathVariable Long id, @RequestParam("arquivo") MultipartFile arquivo) {
        return pastilhaService.salvarImagem(id, arquivo);
    }

    @DeleteMapping("/{id}/imagem")
    @PreAuthorize("hasAuthority('PERM_PASTILHAS')")
    public Pastilha removerImagem(@PathVariable Long id) {
        return pastilhaService.removerImagem(id);
    }
}
