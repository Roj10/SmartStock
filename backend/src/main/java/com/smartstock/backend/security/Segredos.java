package com.smartstock.backend.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chaves do servidor, todas derivadas de um único segredo (SMARTSTOCK_SECRET, no backend/.env):
 * uma assina os tokens de login (JWT) e outra cifra os segredos do 2FA guardados no banco.
 * O segredo nunca fica no código: o repositório é público.
 */
@Component
public class Segredos {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final SecretKey chaveJwt;
    private final SecretKeySpec chaveCifra;

    public Segredos(@Value("${app.seguranca.segredo:}") String segredo) {
        if (segredo == null || segredo.trim().length() < 32 || segredo.trim().startsWith("coloque-")) {
            throw new IllegalStateException("Defina SMARTSTOCK_SECRET no arquivo backend/.env com pelo menos 32 caracteres "
                    + "aleatórios (veja o backend/.env.example). No PowerShell, para gerar um: "
                    + "$b = New-Object byte[] 48; [Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($b); [Convert]::ToBase64String($b)");
        }
        byte[] mestre = segredo.trim().getBytes(StandardCharsets.UTF_8);
        this.chaveJwt = new SecretKeySpec(derivar(mestre, "smartstock-jwt"), "HmacSHA256");
        this.chaveCifra = new SecretKeySpec(derivar(mestre, "smartstock-2fa"), "AES");
    }

    public SecretKey chaveJwt() {
        return chaveJwt;
    }

    /** Cifra com AES-256-GCM (IV aleatório junto do texto); o resultado vai para o banco em Base64. */
    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[12];
            ALEATORIO.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(128, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar.", e);
        }
    }

    public String decifrar(String base64) {
        try {
            byte[] tudo = Base64.getDecoder().decode(base64);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, chaveCifra, new GCMParameterSpec(128, tudo, 0, 12));
            return new String(c.doFinal(tudo, 12, tudo.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível ler o segredo do 2FA (o SMARTSTOCK_SECRET mudou?).", e);
        }
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
