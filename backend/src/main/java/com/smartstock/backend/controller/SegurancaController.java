package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.model.EventoSeguranca;
import com.smartstock.backend.service.AuditoriaService;

/**
 * <b>Em palavras simples:</b> Rota dos eventos de segurança (/api/seguranca/eventos): o "livro de ocorrências" do sistema
 * (logins, erros de senha, bloqueios, ativação do 2FA...). Só o administrador vê.
 *
 * Registro de eventos de segurança (últimos 200): só o administrador.
 */
@RestController
@RequestMapping("/api/seguranca")
@PreAuthorize("hasRole('ADMIN')")
public class SegurancaController {

    private final AuditoriaService auditoria;

    public SegurancaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping("/eventos")
    public List<EventoSeguranca> eventos() {
        return auditoria.recentes();
    }
}
