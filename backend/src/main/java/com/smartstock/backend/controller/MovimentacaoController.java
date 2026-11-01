package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.EntradaLoteRequest;
import com.smartstock.backend.dto.MovimentacaoRequest;
import com.smartstock.backend.model.Movimentacao;
import com.smartstock.backend.service.MovimentacaoService;

import jakarta.validation.Valid;

/**
 * <b>Em palavras simples:</b> Rotas de Movimentações (/api/movimentacoes/...): registrar entradas (compras) e saídas (uso) de
 * estoque e consultar o histórico. Cada movimentação muda a quantidade do item e guarda quem fez.
 */
@RestController
@RequestMapping("/api/movimentacoes")
@PreAuthorize("hasAuthority('PERM_MOVIMENTACOES')")
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
    @ResponseStatus(HttpStatus.CREATED)
    public Movimentacao registrarEntrada(@Valid @RequestBody MovimentacaoRequest request) {
        return movimentacaoService.registrarEntrada(request);
    }

    @PostMapping("/entrada/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Movimentacao> registrarEntradaLote(@Valid @RequestBody EntradaLoteRequest request) {
        return movimentacaoService.registrarEntradaLote(request);
    }

    @PostMapping("/saida")
    @ResponseStatus(HttpStatus.CREATED)
    public Movimentacao registrarSaida(@Valid @RequestBody MovimentacaoRequest request) {
        return movimentacaoService.registrarSaida(request);
    }
}
