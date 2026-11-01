package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloRequest;
import com.smartstock.backend.dto.ModeloProjetoDtos.ModeloResposta;
import com.smartstock.backend.service.ModeloProjetoService;

import jakarta.validation.Valid;

/**
 * <b>Em palavras simples:</b> Rotas dos Modelos de projeto (/api/modelos-projeto/...): "receitas" de checklist e materiais que
 * podem ser reaproveitadas ao criar um projeto novo (ex.: "Portão deslizante padrão").
 *
 * Modelos (padrões) de projeto, usados como ponto de partida ao criar projetos novos.
 */
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

    @GetMapping("/{id}")
    public ModeloResposta buscar(@PathVariable Long id) {
        return modeloService.obter(id);
    }

    /** Salva um modelo pelo nome: cria um novo ou, se o nome já existir, atualiza esse modelo. */
    @PostMapping
    public ModeloResposta salvar(@Valid @RequestBody ModeloRequest request) {
        return modeloService.salvar(request);
    }

    @PutMapping("/{id}")
    public ModeloResposta atualizar(@PathVariable Long id, @Valid @RequestBody ModeloRequest request) {
        return modeloService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        modeloService.excluir(id);
    }
}
