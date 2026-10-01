package com.smartstock.backend.service;

import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.LoginRequest;
import com.smartstock.backend.dto.LoginResponse;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;
import com.smartstock.backend.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
            UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getSenha()));

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities("ROLE_" + usuario.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);

        var permissoes = usuario.getRole() == Role.ADMIN
                ? java.util.Arrays.stream(Modulo.values()).map(Enum::name).collect(Collectors.toSet())
                : usuario.getPermissoes().stream().map(Enum::name).collect(Collectors.toSet());

        return new LoginResponse(token, usuario.getUsername(), usuario.getNome(), usuario.getRole().name(), permissoes);
    }
}
