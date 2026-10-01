package com.smartstock.backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.UsuarioRequest;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.service.UsuarioService;

import jakarta.validation.Valid;

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
    public Usuario criar(@Valid @RequestBody UsuarioRequest request, Authentication authentication) {
        return usuarioService.criar(request, ehAdmin(authentication));
    }

    @PutMapping("/{id}")
    public Usuario atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
            Authentication authentication) {
        return usuarioService.atualizar(id, request, ehAdmin(authentication));
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, Authentication authentication) {
        usuarioService.excluir(id, authentication.getName(), ehAdmin(authentication));
    }

    private boolean ehAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
