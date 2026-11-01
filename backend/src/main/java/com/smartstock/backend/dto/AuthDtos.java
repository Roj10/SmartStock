package com.smartstock.backend.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;

/** Formatos do login em etapas: senha → 2FA (configurar ou digitar o código) → troca de senha (se exigida). */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Próxima etapa do login. */
    public enum Etapa {
        /** Primeira vez: escanear o QR code no app autenticador e confirmar um código. */
        CONFIGURAR_2FA,
        /** Digitar o código de 6 dígitos do app (ou um código de recuperação). */
        CODIGO_2FA,
        /** Trocar a senha (senha de exemplo ou definida pelo administrador). */
        TROCAR_SENHA,
        /** Login concluído: o token de acesso vem junto. */
        CONCLUIDO
    }

    /**
     * Resposta de cada etapa. "desafio" é um token curto (5 min) que só vale para a etapa seguinte;
     * "token" (acesso à API) só vem quando a etapa é CONCLUIDO.
     */
    public record RespostaLogin(Etapa etapa, String desafio, String token, String username, String nome, String role,
            Set<String> permissoes, List<String> codigosRecuperacao) {

        public static RespostaLogin proxima(Etapa etapa, String desafio, String nome, List<String> codigos) {
            return new RespostaLogin(etapa, desafio, null, null, nome, null, null, codigos);
        }
    }

    public record DesafioRequest(@NotBlank String desafio) {
    }

    public record CodigoRequest(@NotBlank String desafio, @NotBlank String codigo) {
    }

    public record TrocarSenhaRequest(@NotBlank String desafio, @NotBlank String novaSenha) {
    }

    /** Dados para configurar o app autenticador. */
    public record ConfiguracaoDoisFatores(String qrCode, String chave, String conta) {
    }
}
