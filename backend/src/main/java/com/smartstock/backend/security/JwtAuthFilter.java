package com.smartstock.backend.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica as chamadas da API pelo token "acesso". Tokens inválidos, vencidos, de outra etapa do login
 * (ex.: o token curto do 2FA) ou de uma sessão já encerrada são ignorados: a chamada segue sem login e
 * recebe 401.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.ler(authHeader.substring(7));
            if (claims != null && JwtService.ACESSO.equals(claims.get("tipo", String.class))) {
                Usuario usuario = usuarioRepository.findByUsername(claims.getSubject()).orElse(null);
                Integer versao = claims.get("ver", Integer.class);
                if (usuario != null && versao != null && versao == usuario.getTokenVersao()
                        && usuario.isDoisFatoresAtivo() && !usuario.isTrocarSenha()) {
                    var authorities = CustomUserDetailsService.buildAuthorities(usuario);
                    var principal = User.withUsername(usuario.getUsername()).password("").authorities(authorities).build();
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
