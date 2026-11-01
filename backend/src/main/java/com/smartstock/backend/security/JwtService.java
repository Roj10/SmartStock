package com.smartstock.backend.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartstock.backend.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Tokens assinados com a chave do servidor (derivada do SMARTSTOCK_SECRET).
 * - "acesso": usado nas chamadas da API, depois do login completo (senha + 2FA).
 * - "setup", "2fa" e "senha": tokens curtos (5 min) que só servem para a etapa seguinte do login.
 * Todos levam a "versão" da conta: trocar a senha, sair ou redefinir o 2FA derruba os tokens antigos.
 */
@Service
public class JwtService {

    public static final String ACESSO = "acesso";
    public static final String CONFIGURAR_2FA = "setup";
    public static final String CODIGO_2FA = "2fa";
    public static final String TROCAR_SENHA = "senha";

    private static final long DESAFIO_MS = 5 * 60 * 1000;

    private final Segredos segredos;
    private final long expiracaoMs;

    public JwtService(Segredos segredos, @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.segredos = segredos;
        this.expiracaoMs = expiracaoMs;
    }

    public String tokenDeAcesso(Usuario usuario) {
        return gerar(usuario, ACESSO, expiracaoMs);
    }

    public String desafio(Usuario usuario, String tipo) {
        return gerar(usuario, tipo, DESAFIO_MS);
    }

    private String gerar(Usuario usuario, String tipo, long validadeMs) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("tipo", tipo)
                .claim("ver", usuario.getTokenVersao())
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validadeMs))
                .signWith(segredos.chaveJwt(), Jwts.SIG.HS256)
                .compact();
    }

    /** Lê e confere a assinatura e a validade; devolve null se o token for inválido ou estiver vencido. */
    public Claims ler(String token) {
        try {
            return Jwts.parser().verifyWith(segredos.chaveJwt()).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
