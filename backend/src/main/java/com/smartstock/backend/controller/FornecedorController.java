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
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.FornecedorRequest;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.service.FornecedorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fornecedores")
@PreAuthorize("hasAuthority('PERM_FORNECEDORES') or hasAuthority('PERM_PASTILHAS') or hasAuthority('PERM_MOVIMENTACOES')")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping
    public List<Fornecedor> listar() {
        return fornecedorService.listar();
    }

    @GetMapping("/{id}")
    public Fornecedor buscar(@PathVariable Long id) {
        return fornecedorService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FORNECEDORES')")
    public Fornecedor criar(@Valid @RequestBody FornecedorRequest request) {
        return fornecedorService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_FORNECEDORES')")
    public Fornecedor atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest request) {
        return fornecedorService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_FORNECEDORES')")
    public void excluir(@PathVariable Long id) {
        fornecedorService.excluir(id);
    }
}
