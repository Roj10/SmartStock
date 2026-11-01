package com.smartstock.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;

/**
 * <b>Em palavras simples:</b> Acesso à tabela de contas (busca pelo nome de usuário no login).
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    long countByRole(Role role);

    List<Usuario> findByRole(Role role);
}
