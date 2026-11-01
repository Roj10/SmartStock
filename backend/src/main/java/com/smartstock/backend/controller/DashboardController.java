package com.smartstock.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.DashboardResponse;
import com.smartstock.backend.service.DashboardService;

/**
 * <b>Em palavras simples:</b> Rota do painel inicial (/api/dashboard): números e alertas mostrados na tela de entrada.
 *
 * Qualquer conta logada pode chamar; o conteúdo se ajusta às abas que a conta tem permissão de ver.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse dashboard(Authentication authentication) {
        return dashboardService.gerar(authentication);
    }
}
