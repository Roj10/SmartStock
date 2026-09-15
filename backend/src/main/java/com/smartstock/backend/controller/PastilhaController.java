package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.PastilhaRequest;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.service.PastilhaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pastilhas")
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
    public Pastilha criar(@Valid @RequestBody PastilhaRequest request) {
        return pastilhaService.criar(request);
    }

    @PutMapping("/{id}")
    public Pastilha atualizar(@PathVariable Long id, @Valid @RequestBody PastilhaRequest request) {
        return pastilhaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        pastilhaService.excluir(id);
    }
}
