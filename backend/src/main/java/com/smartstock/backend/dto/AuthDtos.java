package com.smartstock.backend.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;

/**
 * <b>Em palavras simples:</b> os "formulários" do login em etapas: senha → código enviado por e-mail (2FA)
 * → troca de senha (se exigida).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Próxima etapa do login. */
    public enum Etapa {
        /** Primeira vez (a conta ainda não tem e-mail): informar o e-mail que vai receber os códigos. */
        CADASTRAR_EMAIL,
        /** Digitar o código de 6 dígitos que chegou por e-mail. */
        CODIGO_EMAIL,
        /** Trocar a senha (senha de exemplo ou definida pelo administrador). */
        TROCAR_SENHA,
        /** Login concluído: o token de acesso vem junto. */
        CONCLUIDO
    }

    /**
     * Resposta de cada etapa. "desafio" é um token curto (10 min) que só vale para a etapa seguinte;
     * "token" (acesso à API) só vem quando a etapa é CONCLUIDO. "emailMascarado" mostra para onde o código foi
     * (ex.: re*****@gmail.com), sem revelar o endereço inteiro. "modoTeste" avisa que o e-mail não está
     * configurado no servidor e o código está no terminal do backend.
     */
    public record RespostaLogin(Etapa etapa, String desafio, String token, String username, String nome, String role,
            Set<String> permissoes, String emailMascarado, boolean modoTeste) {

        public static RespostaLogin proxima(Etapa etapa, String desafio, String nome, String emailMascarado,
                boolean modoTeste) {
            return new RespostaLogin(etapa, desafio, null, null, nome, null, null, emailMascarado, modoTeste);
        }
    }

    public record DesafioRequest(@NotBlank String desafio) {
    }

    public record EmailRequest(@NotBlank String desafio, @NotBlank String email) {
    }

    public record CodigoRequest(@NotBlank String desafio, @NotBlank String codigo) {
    }

    public record TrocarSenhaRequest(@NotBlank String desafio, @NotBlank String novaSenha) {
    }
}
