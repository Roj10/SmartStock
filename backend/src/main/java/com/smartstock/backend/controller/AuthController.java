package com.smartstock.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.AuthDtos.CodigoRequest;
import com.smartstock.backend.dto.AuthDtos.ConfiguracaoDoisFatores;
import com.smartstock.backend.dto.AuthDtos.DesafioRequest;
import com.smartstock.backend.dto.AuthDtos.RespostaLogin;
import com.smartstock.backend.dto.AuthDtos.TrocarSenhaRequest;
import com.smartstock.backend.dto.LoginRequest;
import com.smartstock.backend.exception.NaoAutenticadoException;
import com.smartstock.backend.service.AuthService;

import jakarta.validation.Valid;

/**
 * <b>Em palavras simples:</b> Rotas do login (endereços /api/auth/...).
 *
 * Um "controller" é a porta de entrada da API: recebe o pedido que vem do navegador, passa para o
 * "service" fazer o trabalho e devolve a resposta. Aqui ficam as etapas do login:
 * 1) usuário e senha; 2) configurar ou digitar o código do app autenticador (2FA);
 * 3) trocar a senha, quando exigido; e "sair". São as únicas rotas abertas sem login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public RespostaLogin login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/2fa/configurar")
    public ConfiguracaoDoisFatores configurar(@Valid @RequestBody DesafioRequest request) {
        return authService.configurarDoisFatores(request.desafio());
    }

    @PostMapping("/2fa/ativar")
    public RespostaLogin ativar(@Valid @RequestBody CodigoRequest request) {
        return authService.ativarDoisFatores(request.desafio(), request.codigo());
    }

    @PostMapping("/2fa/verificar")
    public RespostaLogin verificar(@Valid @RequestBody CodigoRequest request) {
        return authService.verificarDoisFatores(request.desafio(), request.codigo());
    }

    @PostMapping("/trocar-senha")
    public RespostaLogin trocarSenha(@Valid @RequestBody TrocarSenhaRequest request) {
        return authService.trocarSenha(request.desafio(), request.novaSenha());
    }

    /** Encerra todas as sessões da conta logada. */
    @PostMapping("/sair")
    public void sair(Authentication authentication) {
        if (authentication == null) {
            throw new NaoAutenticadoException("Sessão já encerrada.");
        }
        authService.sair(authentication.getName());
    }
}
