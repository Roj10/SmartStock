package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.FinanceiroDtos.PecaFinanceira;
import com.smartstock.backend.dto.FinanceiroDtos.ProjetoFinanceiro;
import com.smartstock.backend.dto.FinanceiroDtos.Resumo;
import com.smartstock.backend.service.FinanceiroService;

@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasAuthority('PERM_FINANCEIRO')")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @GetMapping("/resumo")
    public Resumo resumo() {
        return financeiroService.resumo();
    }

    @GetMapping("/pecas")
    public List<PecaFinanceira> pecas() {
        return financeiroService.pecas();
    }

    @GetMapping("/projetos")
    public List<ProjetoFinanceiro> projetos() {
        return financeiroService.projetos();
    }
}
