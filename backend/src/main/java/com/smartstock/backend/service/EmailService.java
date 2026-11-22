package com.smartstock.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smartstock.backend.exception.BusinessException;

/**
 * <b>Em palavras simples:</b> o "carteiro" do sistema: envia o código de login para o e-mail da pessoa.
 *
 * <p>Ele conversa diretamente com o servidor de e-mail (protocolo SMTP), do mesmo jeito que o Outlook ou o
 * Thunderbird fazem para enviar uma mensagem. Por padrão usa uma conta do <b>Gmail</b> (smtp.gmail.com), com uma
 * "senha de app" do Google — não a senha normal da conta. A conexão é sempre criptografada (TLS) e o
 * certificado do servidor é conferido, então ninguém no caminho consegue ler o código.
 *
 * <p>Configuração no {@code backend/.env}: {@code MAIL_USUARIO} (o e-mail que envia) e {@code MAIL_SENHA}
 * (a senha de app). Se não estiverem preenchidos, o sistema entra em <b>modo de teste</b>: o código aparece no
 * terminal do backend em vez de ir por e-mail (útil para rodar no computador sem configurar nada).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** Endereço de e-mail aceitável: sem espaços nem quebras de linha (impede "injetar" cabeçalhos no e-mail). */
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+'-]{1,64}@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,24}$");
    private static final int TEMPO_LIMITE_MS = 20_000;

    private final String host;
    private final int porta;
    private final String usuario;
    private final String senha;
    private final String nomeRemetente;

    public EmailService(@Value("${app.email.host:smtp.gmail.com}") String host,
            @Value("${app.email.porta:465}") int porta,
            @Value("${app.email.usuario:}") String usuario,
            @Value("${app.email.senha:}") String senha,
            @Value("${app.email.nome-remetente:SmartStock}") String nomeRemetente) {
        this.host = host.trim();
        this.porta = porta;
        this.usuario = usuario.trim();
        // a senha de app do Google é mostrada em grupos ("abcd efgh ijkl mnop"): os espaços não fazem parte dela
        this.senha = senha.replace(" ", "");
        this.nomeRemetente = nomeRemetente;
        if (modoTeste()) {
            log.warn("E-mail NÃO configurado (MAIL_USUARIO/MAIL_SENHA vazios): os códigos de login vão aparecer neste "
                    + "terminal em vez de serem enviados. Configure o Gmail no backend/.env para uso real.");
        } else {
            log.info("Envio de e-mail configurado: {} via {}:{}", this.usuario, this.host, this.porta);
        }
    }

    /** Sem conta de e-mail configurada, o código só aparece no terminal do backend. */
    public boolean modoTeste() {
        return usuario.isEmpty() || senha.isEmpty();
    }

    /** Confere se o texto parece um endereço de e-mail válido. */
    public static boolean emailValido(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    /** Esconde parte do endereço para mostrar na tela: "renan.silva@gmail.com" vira "re*******@gmail.com". */
    public static String mascarar(String email) {
        int arroba = email.indexOf('@');
        String nome = email.substring(0, arroba);
        String visivel = nome.substring(0, Math.min(2, nome.length()));
        return visivel + "*".repeat(Math.max(3, nome.length() - visivel.length())) + email.substring(arroba);
    }

    /** Envia o e-mail com o código de verificação do login. */
    public void enviarCodigo(String destino, String nomePessoa, String codigo, int minutosValidade) {
        if (!emailValido(destino)) {
            throw new BusinessException("Endereço de e-mail inválido.");
        }
        if (modoTeste()) {
            log.warn("[MODO DE TESTE] Código de login para {} ({}): {}", nomePessoa, destino, codigo);
            return;
        }
        String assunto = codigo + " é o seu código de acesso ao SmartStock";
        String texto = "Olá, " + nomePessoa + "!\r\n\r\n"
                + "Seu código de acesso ao SmartStock é: " + codigo + "\r\n\r\n"
                + "Ele vale por " + minutosValidade + " minutos e só pode ser usado uma vez.\r\n"
                + "Se não foi você que tentou entrar, ignore este e-mail e troque a sua senha: "
                + "alguém pode estar usando ela.\r\n\r\n— SmartStock\r\n";
        String html = "<div style=\"font-family:Segoe UI,Arial,sans-serif;max-width:480px;margin:auto;color:#1f2933\">"
                + "<h2 style=\"color:#1d4e89\">SmartStock</h2>"
                + "<p>Olá, " + escaparHtml(nomePessoa) + "!</p>"
                + "<p>Seu código de acesso é:</p>"
                + "<p style=\"font-size:32px;font-weight:bold;letter-spacing:8px;background:#eef2f6;padding:12px;"
                + "text-align:center;border-radius:8px\">" + codigo + "</p>"
                + "<p>Ele vale por <b>" + minutosValidade + " minutos</b> e só pode ser usado uma vez.</p>"
                + "<p style=\"color:#7b8794;font-size:13px\">Se não foi você que tentou entrar, ignore este e-mail e "
                + "troque a sua senha: alguém pode estar usando ela.</p></div>";
        try {
            enviar(destino, assunto, texto, html);
        } catch (IOException | RuntimeException e) {
            log.error("Falha ao enviar e-mail para {}: {}", destino, e.getMessage());
            throw new BusinessException("Não foi possível enviar o código por e-mail agora. Tente de novo em instantes.");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Conversa com o servidor SMTP. Cada comando recebe uma resposta com um número de 3 dígitos:
    // 2xx/3xx = tudo certo, 4xx/5xx = erro. A ordem é: apresentar-se (EHLO), ligar a criptografia,
    // fazer login (AUTH), dizer quem envia e quem recebe, e mandar a mensagem (DATA).
    // ------------------------------------------------------------------------------------------------

    private void enviar(String destino, String assunto, String texto, String html) throws IOException {
        SSLSocketFactory tls = (SSLSocketFactory) SSLSocketFactory.getDefault();
        Socket socket;
        if (porta == 465) {
            // porta 465: a conexão já começa criptografada (é o que o Gmail recomenda)
            socket = tls.createSocket();
            socket.connect(new InetSocketAddress(host, porta), TEMPO_LIMITE_MS);
            conferirCertificado((SSLSocket) socket);
        } else {
            // porta 587: começa aberta e liga a criptografia com STARTTLS antes de qualquer senha
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, porta), TEMPO_LIMITE_MS);
        }
        socket.setSoTimeout(TEMPO_LIMITE_MS);
        try {
            Conversa c = new Conversa(socket);
            c.esperar(220);
            c.comando("EHLO smartstock", 250);
            if (porta != 465) {
                c.comando("STARTTLS", 220);
                SSLSocket seguro = (SSLSocket) tls.createSocket(socket, host, porta, true);
                conferirCertificado(seguro);
                socket = seguro;
                c = new Conversa(seguro);
                c.comando("EHLO smartstock", 250);
            }
            String credenciais = Base64.getEncoder()
                    .encodeToString(("\0" + usuario + "\0" + senha).getBytes(StandardCharsets.UTF_8));
            c.comandoSigiloso("AUTH PLAIN " + credenciais, 235);
            c.comando("MAIL FROM:<" + usuario + ">", 250);
            c.comando("RCPT TO:<" + destino + ">", 250);
            c.comando("DATA", 354);
            c.escrever(montarMensagem(destino, assunto, texto, html) + "\r\n.\r\n");
            c.esperar(250);
            c.comando("QUIT", 221);
        } finally {
            socket.close();
        }
    }

    /** Obriga a conferir se o certificado é mesmo do servidor de e-mail (impede um intermediário falso). */
    private void conferirCertificado(SSLSocket socket) throws IOException {
        SSLParameters parametros = socket.getSSLParameters();
        parametros.setEndpointIdentificationAlgorithm("HTTPS");
        socket.setSSLParameters(parametros);
        socket.startHandshake();
    }

    /** Monta o e-mail com duas versões (texto simples e HTML); o programa de e-mail escolhe qual mostrar. */
    private String montarMensagem(String destino, String assunto, String texto, String html) {
        String divisor = "smartstock-" + UUID.randomUUID();
        String dominio = usuario.substring(usuario.indexOf('@') + 1);
        return "From: " + codificar(nomeRemetente) + " <" + usuario + ">\r\n"
                + "To: <" + destino + ">\r\n"
                + "Subject: " + codificar(assunto) + "\r\n"
                + "Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\r\n"
                + "Message-ID: <" + UUID.randomUUID() + "@" + dominio + ">\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/alternative; boundary=\"" + divisor + "\"\r\n"
                + "\r\n"
                + "--" + divisor + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + base64EmLinhas(texto) + "\r\n"
                + "--" + divisor + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + base64EmLinhas(html) + "\r\n"
                + "--" + divisor + "--";
    }

    /** Assunto e nome com acentos precisam ser codificados para o e-mail chegar sem caracteres estranhos. */
    private static String codificar(String texto) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private static String base64EmLinhas(String texto) {
        return Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Lê e escreve as linhas da conversa com o servidor SMTP. */
    private static final class Conversa {
        private final BufferedReader entrada;
        private final OutputStream saida;

        Conversa(Socket socket) throws IOException {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.saida = socket.getOutputStream();
        }

        void escrever(String texto) throws IOException {
            saida.write(texto.getBytes(StandardCharsets.UTF_8));
            saida.flush();
        }

        void comando(String linha, int esperado) throws IOException {
            escrever(linha + "\r\n");
            esperar(esperado, linha);
        }

        /** Igual a {@link #comando}, mas não repete o comando (que contém a senha) numa mensagem de erro. */
        void comandoSigiloso(String linha, int esperado) throws IOException {
            escrever(linha + "\r\n");
            esperar(esperado, "AUTH (login na conta de e-mail)");
        }

        void esperar(int esperado) throws IOException {
            esperar(esperado, "conexão");
        }

        /** Respostas podem ter várias linhas ("250-..."); a última tem espaço depois do número ("250 ..."). */
        private void esperar(int esperado, String etapa) throws IOException {
            String linha;
            do {
                linha = entrada.readLine();
                if (linha == null) {
                    throw new IOException("o servidor de e-mail encerrou a conexão em: " + etapa);
                }
            } while (linha.length() > 3 && linha.charAt(3) == '-');
            int codigo;
            try {
                codigo = Integer.parseInt(linha.substring(0, 3));
            } catch (RuntimeException e) {
                throw new IOException("resposta inesperada do servidor de e-mail: " + linha);
            }
            if (codigo != esperado) {
                String dica = codigo == 535 ? " (usuário ou senha de app do e-mail incorretos)" : "";
                throw new IOException("servidor de e-mail respondeu \"" + linha + "\" em " + etapa.toLowerCase(Locale.ROOT) + dica);
            }
        }
    }
}
