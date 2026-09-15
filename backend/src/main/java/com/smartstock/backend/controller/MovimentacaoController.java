package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.MovimentacaoRequest;
import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.service.MovimentacaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping
    public List<Movimentacao> listar(@RequestParam(required = false) Long pastilhaId) {
        if (pastilhaId != null) {
            return movimentacaoService.listarPorPastilha(pastilhaId);
        }
        return movimentacaoService.listar();
    }

    @PostMapping("/entrada")
    public Movimentacao registrarEntrada(@Valid @RequestBody MovimentacaoRequest request) {
        return movimentacaoService.registrarEntrada(request);
    }

    @PostMapping("/saida")
    public Movimentacao registrarSaida(@Valid @RequestBody MovimentacaoRequest request) {
        return movimentacaoService.registrarSaida(request);
    }
}
