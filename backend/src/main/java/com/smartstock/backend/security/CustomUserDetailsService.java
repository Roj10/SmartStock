package com.smartstock.backend.security;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getUsername())
                .password(usuario.getSenha())
                .authorities(buildAuthorities(usuario))
                .build();
    }

    public static List<GrantedAuthority> buildAuthorities(Usuario usuario) {
        Stream<String> roleAuthority = Stream.of("ROLE_" + usuario.getRole().name());

        // Administradores têm acesso implícito a todos os módulos do sistema.
        Stream<String> moduloAuthorities = usuario.getRole() == Role.ADMIN
                ? Stream.of(Modulo.values()).map(m -> "PERM_" + m.name())
                : usuario.getPermissoes().stream().map(m -> "PERM_" + m.name());

        return Stream.concat(roleAuthority, moduloAuthorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
