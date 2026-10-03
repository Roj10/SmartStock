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

import com.smartstock.backend.dto.FinanceiroDtos.VendaResposta;
import com.smartstock.backend.dto.VendaRequest;
import com.smartstock.backend.service.VendaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vendas")
@PreAuthorize("hasAuthority('PERM_FINANCEIRO')")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping
    public List<VendaResposta> listar() {
        return vendaService.listar();
    }

    @PostMapping
    public VendaResposta criar(@Valid @RequestBody VendaRequest request) {
        return vendaService.criar(request);
    }

    @PutMapping("/{id}")
    public VendaResposta atualizar(@PathVariable Long id, @Valid @RequestBody VendaRequest request) {
        return vendaService.atualizar(id, request);
    }

    @PostMapping("/{id}/fechar")
    public VendaResposta fechar(@PathVariable Long id) {
        return vendaService.fechar(id);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        vendaService.excluir(id);
    }
}
