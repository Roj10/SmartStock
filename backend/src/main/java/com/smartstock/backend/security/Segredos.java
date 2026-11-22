package com.smartstock.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Em palavras simples:</b> as chaves do servidor, todas tiradas de um único segredo (SMARTSTOCK_SECRET,
 * no backend/.env). Uma assina os "crachás" de login (tokens JWT); a outra gera a "impressão digital"
 * dos códigos enviados por e-mail, para o banco nunca guardar o código em si.
 * O segredo nunca fica no código: o repositório é público.
 */
@Component
public class Segredos {

    private final SecretKey chaveJwt;
    private final SecretKeySpec chaveCodigos;

    public Segredos(@Value("${app.seguranca.segredo:}") String segredo) {
        if (segredo == null || segredo.trim().length() < 32 || segredo.trim().startsWith("coloque-")) {
            throw new IllegalStateException("Defina SMARTSTOCK_SECRET no arquivo backend/.env com pelo menos 32 caracteres "
                    + "aleatórios (veja o backend/.env.example). No PowerShell, para gerar um: "
                    + "$b = New-Object byte[] 48; [Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($b); [Convert]::ToBase64String($b)");
        }
        byte[] mestre = segredo.trim().getBytes(StandardCharsets.UTF_8);
        this.chaveJwt = new SecretKeySpec(derivar(mestre, "smartstock-jwt"), "HmacSHA256");
        this.chaveCodigos = new SecretKeySpec(derivar(mestre, "smartstock-codigo-email"), "HmacSHA256");
    }

    public SecretKey chaveJwt() {
        return chaveJwt;
    }

    /**
     * "Impressão digital" (HMAC-SHA256) de um código de login, ligada à conta. Mesmo quem tiver uma cópia do banco
     * não consegue descobrir o código a partir dela sem o SMARTSTOCK_SECRET.
     */
    public String impressaoDigital(String username, String codigo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chaveCodigos);
            byte[] resultado = mac.doFinal((username.toLowerCase(java.util.Locale.ROOT) + ":" + codigo)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Compara duas impressões digitais em tempo constante (não deixa medir "quantos caracteres acertou"). */
    public static boolean iguais(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] derivar(byte[] mestre, String finalidade) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(mestre, "HmacSHA256"));
            return mac.doFinal(finalidade.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
