package com.smartstock.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.repository.UsuarioRepository;

/**
 * Confere a senha de um administrador antes de ações sensíveis (ex.: editar ou excluir um projeto do Progresso).
 * Vale a senha de qualquer conta com perfil de administrador.
 */
@Service
public class SenhaAdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public SenhaAdminService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lança erro (422, com mensagem) se a senha estiver em branco ou não for de nenhum administrador. */
    public void exigir(String senha, String acao) {
        if (senha == null || senha.isBlank()) {
            throw new BusinessException("Informe a senha do administrador para " + acao + ".");
        }
        boolean confere = usuarioRepository.findByRole(Role.ADMIN).stream()
                .anyMatch(admin -> passwordEncoder.matches(senha, admin.getSenha()));
        if (!confere) {
            throw new BusinessException("Senha do administrador incorreta.");
        }
    }
}
