package com.smartstock.backend.security;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * Gerador mínimo de QR code (modo byte, correção de erro nível M), sem bibliotecas externas.
 * Usado só para mostrar o endereço "otpauth://" do 2FA, que o app autenticador lê pela câmera.
 * Segue o padrão ISO/IEC 18004 (mesma estrutura do gerador de referência de Project Nayuki).
 */
public final class QrCode {

    // nível M, índice = versão (1..40)
    private static final int[] ECC_POR_BLOCO = { -1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28,
            28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28 };
    private static final int[] BLOCOS = { -1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17,
            17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49 };
    private static final int FORMATO_M = 0;

    private final int versao;
    private final int tamanho;
    private final boolean[][] modulos;
    private final boolean[][] funcao;

    private QrCode(int versao) {
        this.versao = versao;
        this.tamanho = versao * 4 + 17;
        this.modulos = new boolean[tamanho][tamanho];
        this.funcao = new boolean[tamanho][tamanho];
    }

    /** Imagem PNG do QR code como "data:image/png;base64,...", pronta para um &lt;img&gt;. */
    public static String pngDataUrl(String texto, int pixelsPorModulo) {
        QrCode qr = gerar(texto.getBytes(StandardCharsets.UTF_8));
        int borda = 4;
        int lado = (qr.tamanho + borda * 2) * pixelsPorModulo;
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                int mx = x / pixelsPorModulo - borda;
                int my = y / pixelsPorModulo - borda;
                boolean escuro = mx >= 0 && my >= 0 && mx < qr.tamanho && my < qr.tamanho && qr.modulos[my][mx];
                img.setRGB(x, y, escuro ? 0x000000 : 0xFFFFFF);
            }
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static QrCode gerar(byte[] dados) {
        int versao = 1;
        for (;; versao++) {
            if (versao > 40) {
                throw new IllegalArgumentException("Texto grande demais para um QR code");
            }
            int bitsContagem = versao <= 9 ? 8 : 16;
            if (4 + bitsContagem + dados.length * 8 <= palavrasDeDados(versao) * 8) {
                break;
            }
        }
        // monta o fluxo de bits: modo byte (0100), tamanho, dados, terminador e preenchimento
        Bits bits = new Bits();
        bits.add(0b0100, 4);
        bits.add(dados.length, versao <= 9 ? 8 : 16);
        for (byte b : dados) {
            bits.add(b & 0xFF, 8);
        }
        int capacidade = palavrasDeDados(versao) * 8;
        bits.add(0, Math.min(4, capacidade - bits.tamanho));
        bits.add(0, (8 - bits.tamanho % 8) % 8);
        for (int pad = 0xEC; bits.tamanho < capacidade; pad ^= 0xEC ^ 0x11) {
            bits.add(pad, 8);
        }
        byte[] palavras = new byte[bits.tamanho / 8];
        for (int i = 0; i < bits.tamanho; i++) {
            palavras[i >>> 3] |= (byte) (bits.get(i) << (7 - (i & 7)));
        }

        QrCode qr = new QrCode(versao);
        qr.desenharPadroesFixos();
        qr.desenharPalavras(intercalarComCorrecao(palavras, versao));

        // escolhe a máscara com menor penalidade
        int melhor = 0;
        long menorPenalidade = Long.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            qr.aplicarMascara(m);
            qr.desenharFormato(m);
            long p = qr.penalidade();
            if (p < menorPenalidade) {
                menorPenalidade = p;
                melhor = m;
            }
            qr.aplicarMascara(m); // desfaz (XOR)
        }
        qr.aplicarMascara(melhor);
        qr.desenharFormato(melhor);
        return qr;
    }

    private static int modulosBrutos(int ver) {
        int r = (16 * ver + 128) * ver + 64;
        if (ver >= 2) {
            int n = ver / 7 + 2;
            r -= (25 * n - 10) * n - 55;
            if (ver >= 7) {
                r -= 36;
            }
        }
        return r;
    }

    private static int palavrasDeDados(int ver) {
        return modulosBrutos(ver) / 8 - ECC_POR_BLOCO[ver] * BLOCOS[ver];
    }

    private static byte[] intercalarComCorrecao(byte[] dados, int ver) {
        int numBlocos = BLOCOS[ver];
        int eccLen = ECC_POR_BLOCO[ver];
        int total = modulosBrutos(ver) / 8;
        int curtos = numBlocos - total % numBlocos;
        int tamCurto = total / numBlocos;
        byte[][] blocos = new byte[numBlocos][];
        byte[] divisor = divisorRs(eccLen);
        for (int i = 0, k = 0; i < numBlocos; i++) {
            int len = tamCurto - eccLen + (i < curtos ? 0 : 1);
            byte[] dat = java.util.Arrays.copyOfRange(dados, k, k + len);
            k += len;
            byte[] ecc = restoRs(dat, divisor);
            byte[] bloco = new byte[tamCurto + 1];
            System.arraycopy(dat, 0, bloco, 0, dat.length);
            // blocos curtos ganham uma posição vazia para alinhar com os longos
            int pos = (i < curtos) ? dat.length + 1 : dat.length;
            System.arraycopy(ecc, 0, bloco, pos, ecc.length);
            blocos[i] = bloco;
        }
        byte[] r = new byte[total];
        int n = 0;
        for (int i = 0; i < blocos[0].length; i++) {
            for (int j = 0; j < numBlocos; j++) {
                if (i != tamCurto - eccLen || j >= curtos) {
                    r[n++] = blocos[j][i];
                }
            }
        }
        return r;
    }

    private static byte[] divisorRs(int grau) {
        byte[] r = new byte[grau];
        r[grau - 1] = 1;
        int raiz = 1;
        for (int i = 0; i < grau; i++) {
            for (int j = 0; j < r.length; j++) {
                r[j] = (byte) mul(r[j] & 0xFF, raiz);
                if (j + 1 < r.length) {
                    r[j] ^= r[j + 1];
                }
            }
            raiz = mul(raiz, 0x02);
        }
        return r;
    }

    private static byte[] restoRs(byte[] dados, byte[] divisor) {
        byte[] r = new byte[divisor.length];
        for (byte b : dados) {
            int fator = (b ^ r[0]) & 0xFF;
            System.arraycopy(r, 1, r, 0, r.length - 1);
            r[r.length - 1] = 0;
            for (int i = 0; i < r.length; i++) {
                r[i] ^= (byte) mul(divisor[i] & 0xFF, fator);
            }
        }
        return r;
    }

    private static int mul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    private void set(int x, int y, boolean escuro) {
        modulos[y][x] = escuro;
        funcao[y][x] = true;
    }

    private void desenharPadroesFixos() {
        for (int i = 0; i < tamanho; i++) {
            set(6, i, i % 2 == 0);
            set(i, 6, i % 2 == 0);
        }
        localizador(3, 3);
        localizador(tamanho - 4, 3);
        localizador(3, tamanho - 4);
        int[] pos = posicoesAlinhamento();
        int n = pos.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!(i == 0 && j == 0 || i == 0 && j == n - 1 || i == n - 1 && j == 0)) {
                    alinhamento(pos[i], pos[j]);
                }
            }
        }
        desenharFormato(0);
        desenharVersao();
    }

    private void localizador(int x, int y) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int d = Math.max(Math.abs(dx), Math.abs(dy));
                int xx = x + dx;
                int yy = y + dy;
                if (xx >= 0 && xx < tamanho && yy >= 0 && yy < tamanho) {
                    set(xx, yy, d != 2 && d != 4);
                }
            }
        }
    }

    private void alinhamento(int x, int y) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                set(x + dx, y + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private int[] posicoesAlinhamento() {
        if (versao == 1) {
            return new int[0];
        }
        int n = versao / 7 + 2;
        int passo = (versao == 32) ? 26 : (versao * 4 + n * 2 + 1) / (n * 2 - 2) * 2;
        int[] r = new int[n];
        r[0] = 6;
        for (int i = n - 1, p = tamanho - 7; i >= 1; i--, p -= passo) {
            r[i] = p;
        }
        return r;
    }

    private void desenharFormato(int mascara) {
        int dados = FORMATO_M << 3 | mascara;
        int rem = dados;
        for (int i = 0; i < 10; i++) {
            rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        }
        int bits = (dados << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) {
            set(8, i, bit(bits, i));
        }
        set(8, 7, bit(bits, 6));
        set(8, 8, bit(bits, 7));
        set(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) {
            set(14 - i, 8, bit(bits, i));
        }
        for (int i = 0; i < 8; i++) {
            set(tamanho - 1 - i, 8, bit(bits, i));
        }
        for (int i = 8; i < 15; i++) {
            set(8, tamanho - 15 + i, bit(bits, i));
        }
        set(8, tamanho - 8, true);
    }

    private void desenharVersao() {
        if (versao < 7) {
            return;
        }
        int rem = versao;
        for (int i = 0; i < 12; i++) {
            rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        }
        int bits = versao << 12 | rem;
        for (int i = 0; i < 18; i++) {
            boolean b = bit(bits, i);
            int a = tamanho - 11 + i % 3;
            int c = i / 3;
            set(a, c, b);
            set(c, a, b);
        }
    }

    private void desenharPalavras(byte[] dados) {
        int i = 0;
        for (int direita = tamanho - 1; direita >= 1; direita -= 2) {
            if (direita == 6) {
                direita = 5;
            }
            for (int vert = 0; vert < tamanho; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = direita - j;
                    boolean subindo = ((direita + 1) & 2) == 0;
                    int y = subindo ? tamanho - 1 - vert : vert;
                    if (!funcao[y][x] && i < dados.length * 8) {
                        modulos[y][x] = bit(dados[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void aplicarMascara(int m) {
        for (int y = 0; y < tamanho; y++) {
            for (int x = 0; x < tamanho; x++) {
                boolean inverter = switch (m) {
                    case 0 -> (x + y) % 2 == 0;
                    case 1 -> y % 2 == 0;
                    case 2 -> x % 3 == 0;
                    case 3 -> (x + y) % 3 == 0;
                    case 4 -> (x / 3 + y / 2) % 2 == 0;
                    case 5 -> x * y % 2 + x * y % 3 == 0;
                    case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
                    default -> ((x + y) % 2 + x * y % 3) % 2 == 0;
                };
                if (inverter && !funcao[y][x]) {
                    modulos[y][x] = !modulos[y][x];
                }
            }
        }
    }

    /** Penalidade simplificada (sequências longas, blocos 2x2 e equilíbrio claro/escuro). */
    private long penalidade() {
        long p = 0;
        for (int y = 0; y < tamanho; y++) {
            p += sequencias(true, y);
            p += sequencias(false, y);
        }
        int escuros = 0;
        for (int y = 0; y < tamanho; y++) {
            for (int x = 0; x < tamanho; x++) {
                if (modulos[y][x]) {
                    escuros++;
                }
                if (x + 1 < tamanho && y + 1 < tamanho) {
                    boolean c = modulos[y][x];
                    if (c == modulos[y][x + 1] && c == modulos[y + 1][x] && c == modulos[y + 1][x + 1]) {
                        p += 3;
                    }
                }
            }
        }
        int total = tamanho * tamanho;
        int k = (Math.abs(escuros * 20 - total * 10) + total - 1) / total - 1;
        return p + Math.max(0, k) * 10L;
    }

    private long sequencias(boolean linha, int i) {
        long p = 0;
        int corrida = 1;
        for (int j = 1; j < tamanho; j++) {
            boolean a = linha ? modulos[i][j] : modulos[j][i];
            boolean b = linha ? modulos[i][j - 1] : modulos[j - 1][i];
            if (a == b) {
                corrida++;
                if (corrida == 5) {
                    p += 3;
                } else if (corrida > 5) {
                    p++;
                }
            } else {
                corrida = 1;
            }
        }
        return p;
    }

    private static boolean bit(int x, int i) {
        return ((x >>> i) & 1) != 0;
    }

    private static final class Bits {
        private final java.util.BitSet bits = new java.util.BitSet();
        private int tamanho;

        void add(int valor, int n) {
            for (int i = n - 1; i >= 0; i--, tamanho++) {
                bits.set(tamanho, ((valor >>> i) & 1) != 0);
            }
        }

        int get(int i) {
            return bits.get(i) ? 1 : 0;
        }
    }
}
