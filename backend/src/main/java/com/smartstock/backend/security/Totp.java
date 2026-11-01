package com.smartstock.backend.security;

import java.nio.ByteBuffer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.OptionalLong;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Códigos de 6 dígitos do app autenticador (TOTP, RFC 6238): HMAC-SHA1, intervalos de 30 segundos.
 * É o padrão usado pelo Google Authenticator, Microsoft Authenticator e Authy.
 */
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom ALEATORIO = new SecureRandom();
    public static final int PERIODO = 30;

    private Totp() {
    }

    /** Novo segredo de 160 bits, em Base32 (é o que o app guarda). */
    public static String novoSegredo() {
        byte[] b = new byte[20];
        ALEATORIO.nextBytes(b);
        return base32(b);
    }

    /** Endereço "otpauth://" que o app lê pelo QR code. */
    public static String uri(String emissor, String conta, String segredo) {
        String rotulo = enc(emissor) + ":" + enc(conta);
        return "otpauth://totp/" + rotulo + "?secret=" + segredo + "&issuer=" + enc(emissor)
                + "&algorithm=SHA1&digits=6&period=" + PERIODO;
    }

    /**
     * Confere o código aceitando 1 intervalo antes ou depois (relógio do celular um pouco adiantado/atrasado).
     * Devolve o número do intervalo usado, para impedir que o mesmo código seja usado de novo.
     */
    public static OptionalLong verificar(String segredo, String codigo, long agoraSegundos) {
        String limpo = codigo == null ? "" : codigo.replaceAll("\\s", "");
        if (!limpo.matches("\\d{6}")) {
            return OptionalLong.empty();
        }
        byte[] chave = deBase32(segredo);
        long passo = agoraSegundos / PERIODO;
        for (long p = passo - 1; p <= passo + 1; p++) {
            if (constante(gerar(chave, p), limpo)) {
                return OptionalLong.of(p);
            }
        }
        return OptionalLong.empty();
    }

    static String gerar(byte[] chave, long passo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(chave, "HmacSHA1"));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(passo).array());
            int o = h[h.length - 1] & 0x0F;
            int bin = ((h[o] & 0x7F) << 24) | ((h[o + 1] & 0xFF) << 16) | ((h[o + 2] & 0xFF) << 8) | (h[o + 3] & 0xFF);
            return String.format("%06d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Código atual (usado só nos testes). */
    public static String codigoAtual(String segredo, long agoraSegundos) {
        return gerar(deBase32(segredo), agoraSegundos / PERIODO);
    }

    private static boolean constante(String a, String b) {
        return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    static String base32(byte[] dados) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte d : dados) {
            buffer = (buffer << 8) | (d & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return sb.toString();
    }

    static byte[] deBase32(String texto) {
        String t = texto.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(t.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : t.toCharArray()) {
            int v = BASE32.indexOf(c);
            if (v < 0) {
                throw new IllegalArgumentException("Segredo inválido");
            }
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) ((buffer >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        return out.array();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
