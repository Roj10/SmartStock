package com.smartstock.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.AuthDtos.CodigoRequest;
import com.smartstock.backend.dto.AuthDtos.DesafioRequest;
import com.smartstock.backend.dto.AuthDtos.EmailRequest;
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
 * 1) usuário e senha; 2) código de 6 dígitos enviado por e-mail (2FA) — na primeira vez, antes, a pessoa
 * informa o e-mail;
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

    /** Primeira vez: a pessoa informa o e-mail; um código é enviado para ele. */
    @PostMapping("/email/cadastrar")
    public RespostaLogin cadastrarEmail(@Valid @RequestBody EmailRequest request) {
        return authService.cadastrarEmail(request.desafio(), request.email());
    }

    /** Confere o código de 6 dígitos que chegou por e-mail. */
    @PostMapping("/email/verificar")
    public RespostaLogin verificarCodigo(@Valid @RequestBody CodigoRequest request) {
        return authService.verificarCodigo(request.desafio(), request.codigo());
    }

    /** Envia um código novo (o anterior deixa de valer). Só uma vez por minuto. */
    @PostMapping("/email/reenviar")
    public RespostaLogin reenviarCodigo(@Valid @RequestBody DesafioRequest request) {
        return authService.reenviarCodigo(request.desafio());
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
