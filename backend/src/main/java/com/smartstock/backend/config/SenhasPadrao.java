package com.smartstock.backend.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartstock.backend.repository.UsuarioRepository;

/**
 * As contas de exemplo foram criadas com senhas publicadas no README (admin123, operador123, financeiro123).
 * Enquanto alguma delas ainda usar essa senha, a pessoa é obrigada a trocá-la no próximo login.
 */
@Component
@Order(2)
public class SenhasPadrao implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SenhasPadrao.class);
    private static final Map<String, String> PADRAO = Map.of("admin", "admin123", "operador", "operador123",
            "financeiro", "financeiro123");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public SenhasPadrao(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        PADRAO.forEach((username, senha) -> usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (!u.isTrocarSenha() && passwordEncoder.matches(senha, u.getSenha())) {
                u.setTrocarSenha(true);
                usuarioRepository.save(u);
                log.warn("A conta \"{}\" ainda usa a senha de exemplo: a troca será exigida no próximo login.", username);
            }
        }));
    }
}
