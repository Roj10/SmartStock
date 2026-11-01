package com.smartstock.backend.service;

import java.util.Locale;
import java.util.Set;

import com.smartstock.backend.exception.BusinessException;

/** Regras mínimas para senhas novas. */
public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 8;

    /** Senhas dos dados de exemplo (publicadas no README) e outras óbvias: nunca aceitas como senha nova. */
    public static final Set<String> PROIBIDAS = Set.of("admin123", "operador123", "financeiro123", "12345678",
            "123456789", "senha123", "password", "smartstock", "smartstock123", "qwerty123", "abc12345");

    private PoliticaSenha() {
    }

    public static void validar(String senha, String username) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw new BusinessException("A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        if (senha.length() > 72) {
            throw new BusinessException("A senha pode ter no máximo 72 caracteres.");
        }
        if (!senha.matches(".*\\p{L}.*") || !senha.matches(".*\\d.*")) {
            throw new BusinessException("A senha deve ter letras e números.");
        }
        String minuscula = senha.toLowerCase(Locale.ROOT);
        if (PROIBIDAS.contains(minuscula) || (username != null && username.length() >= 4 && minuscula.contains(username.toLowerCase(Locale.ROOT)))) {
            throw new BusinessException("Essa senha é fácil de adivinhar (é de exemplo ou contém o nome de usuário). "
                    + "Escolha outra.");
        }
    }
}
