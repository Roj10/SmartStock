package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.UsuarioRequest;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.service.UsuarioService;

import jakarta.validation.Valid;

/**
 * <b>Em palavras simples:</b> Rotas de Usuários (/api/usuarios/...): criar, listar, editar e excluir contas, escolher as abas
 * de cada uma e redefinir o 2FA de quem perdeu o celular.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_USUARIOS')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario criar(@Valid @RequestBody UsuarioRequest request, Authentication authentication) {
        return usuarioService.criar(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public Usuario atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
            Authentication authentication) {
        return usuarioService.atualizar(id, request, authentication.getName());
    }

    /** Desliga o 2FA da conta (ex.: celular perdido); só o administrador. */
    @PostMapping("/{id}/redefinir-2fa")
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario redefinirDoisFatores(@PathVariable Long id, Authentication authentication) {
        return usuarioService.redefinirDoisFatores(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id, Authentication authentication) {
        usuarioService.excluir(id, authentication.getName());
    }
}
