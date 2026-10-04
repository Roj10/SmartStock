package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloRequest;
import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloResposta;
import com.smartstock.backend.service.ModeloProjetoService;

import jakarta.validation.Valid;

/** Modelos (padrões) de projeto, usados como ponto de partida ao criar projetos novos. */
@RestController
@RequestMapping("/api/modelos-projeto")
@PreAuthorize("hasAuthority('PERM_PROJETOS')")
public class ModeloProjetoController {

    private final ModeloProjetoService modeloService;

    public ModeloProjetoController(ModeloProjetoService modeloService) {
        this.modeloService = modeloService;
    }

    @GetMapping
    public List<ModeloResposta> listar() {
        return modeloService.listar();
    }

    @PostMapping
    public ModeloResposta salvar(@Valid @RequestBody ModeloRequest request) {
        return modeloService.salvar(request);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        modeloService.excluir(id);
    }
}
