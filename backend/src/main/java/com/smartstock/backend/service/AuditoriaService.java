package com.smartstock.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.smartstock.backend.model.EventoSeguranca;
import com.smartstock.backend.repository.EventoSegurancaRepository;

/** Grava os eventos de segurança. Nunca grava senhas, códigos ou tokens. */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final EventoSegurancaRepository repository;

    public AuditoriaService(EventoSegurancaRepository repository) {
        this.repository = repository;
    }

    /** Grava numa transação própria: o registro fica mesmo quando a operação principal é recusada. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tipo, String usuario, String detalhe) {
        try {
            EventoSeguranca e = new EventoSeguranca();
            e.setTipo(tipo);
            e.setUsuario(cortar(usuario, 100));
            e.setDetalhe(cortar(detalhe, 300));
            e.setIp(ipAtual());
            repository.save(e);
        } catch (RuntimeException ex) {
            log.warn("Não foi possível registrar o evento de segurança {}: {}", tipo, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<EventoSeguranca> recentes() {
        return repository.findTop200ByOrderByDataHoraDescIdDesc();
    }

    private static String ipAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }

    private static String cortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
