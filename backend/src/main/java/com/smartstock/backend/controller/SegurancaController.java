package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.model.EventoSeguranca;
import com.smartstock.backend.service.AuditoriaService;

/** Registro de eventos de segurança (últimos 200): só o administrador. */
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
