package com.smartstock.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.repository.UsuarioRepository;
import com.smartstock.backend.security.TentativasService;

/**
 * Confere a senha de um administrador antes de ações sensíveis (ex.: editar ou excluir um projeto do Progresso).
 * Vale a senha de qualquer conta com perfil de administrador.
 */
@Service
public class SenhaAdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TentativasService tentativas;
    private final AuditoriaService auditoria;

    public SenhaAdminService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            TentativasService tentativas, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tentativas = tentativas;
        this.auditoria = auditoria;
    }

    /** Lança erro (422, com mensagem) se a senha estiver em branco ou não for de nenhum administrador. */
    public void exigir(String senha, String acao) {
        if (senha == null || senha.isBlank()) {
            throw new BusinessException("Informe a senha do administrador para " + acao + ".");
        }
        // limite de tentativas por conta que está pedindo (impede adivinhar a senha do administrador)
        String quem = usuarioLogado();
        String chave = "senha-admin:" + quem;
        tentativas.verificar(chave);
        boolean confere = usuarioRepository.findByRole(Role.ADMIN).stream()
                .anyMatch(admin -> passwordEncoder.matches(senha, admin.getSenha()));
        if (!confere) {
            boolean bloqueou = tentativas.falhou(chave);
            auditoria.registrar(bloqueou ? "SENHA_ADMIN_BLOQUEADA" : "SENHA_ADMIN_FALHA", quem,
                    "Senha do administrador incorreta ao " + acao);
            throw new BusinessException("Senha do administrador incorreta.");
        }
        tentativas.acertou(chave);
        auditoria.registrar("SENHA_ADMIN_USADA", quem, "Autorizado: " + acao);
    }

    private static String usuarioLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "desconhecido";
    }
}
