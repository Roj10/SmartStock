package com.smartstock.backend.storage;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.backend.exception.ResourceNotFoundException;

import jakarta.annotation.PostConstruct;

/**
 * Arquivos no Supabase Storage, pela API REST dele. A chave secreta (service_role) fica só no backend
 * (arquivo .env) e nunca vai para o navegador.
 */
@Component
@ConditionalOnProperty(name = "app.armazenamento", havingValue = "supabase", matchIfMissing = true)
public class SupabaseStorage implements Armazenamento {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorage.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper json = new ObjectMapper();

    private final String base;
    private final String chave;

    public SupabaseStorage(@Value("${app.supabase.url}") String url,
            @Value("${app.supabase.service-key}") String chave) {
        this.base = (url.endsWith("/") ? url.substring(0, url.length() - 1) : url) + "/storage/v1";
        this.chave = chave.trim();
    }

    /** Cria os buckets na primeira execução (e confere, logo ao iniciar, se a chave e o endereço funcionam). */
    @PostConstruct
    public void prepararBuckets() {
        garantirBucket(PASTILHAS, true);
        garantirBucket(MENSAGENS, false);
        garantirBucket(PROJETOS, false);
        log.info("Supabase Storage pronto ({}): buckets {}, {} e {}", base, PASTILHAS, MENSAGENS, PROJETOS);
    }

    private void garantirBucket(String id, boolean publico) {
        HttpResponse<String> existe = enviarTexto(req("/bucket/" + id).GET());
        if (existe.statusCode() == 200) {
            return;
        }
        if (existe.statusCode() == 401 || existe.statusCode() == 403) {
            throw new IllegalStateException("O Supabase recusou a chave SUPABASE_SERVICE_KEY (HTTP " + existe.statusCode()
                    + "). Use a chave secreta (secret / service_role) em Project Settings > API Keys. Resposta: "
                    + existe.body());
        }
        String corpo = paraJson(Map.of("id", id, "name", id, "public", publico));
        HttpResponse<String> criado = enviarTexto(req("/bucket").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)));
        if (criado.statusCode() >= 300 && !criado.body().contains("already exists")) {
            throw new IllegalStateException("Não foi possível criar o bucket \"" + id + "\" no Supabase Storage (HTTP "
                    + criado.statusCode() + "): " + criado.body());
        }
        log.info("Bucket \"{}\" criado no Supabase Storage ({})", id, publico ? "público" : "privado");
    }

    @Override
    public void enviar(String bucket, String caminho, byte[] conteudo, String tipo) {
        HttpResponse<String> r = enviarTexto(req("/object/" + bucket + "/" + codificar(caminho))
                .header("Content-Type", tipo)
                .header("x-upsert", "true")
                .header("cache-control", "max-age=31536000")
                .POST(HttpRequest.BodyPublishers.ofByteArray(conteudo)));
        if (r.statusCode() >= 300) {
            throw new UncheckedIOException(new IOException("Falha ao enviar o arquivo para o Supabase Storage (HTTP "
                    + r.statusCode() + "): " + r.body()));
        }
    }

    @Override
    public byte[] baixar(String bucket, String caminho) {
        try {
            HttpResponse<byte[]> r = http.send(req("/object/" + bucket + "/" + codificar(caminho)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() == 200) {
                return r.body();
            }
            if (r.statusCode() == 400 || r.statusCode() == 404) {
                throw new ResourceNotFoundException("Arquivo não encontrado.");
            }
            throw new IOException("Falha ao baixar o arquivo do Supabase Storage (HTTP " + r.statusCode() + ")");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void apagar(String bucket, String caminho) {
        try {
            enviarTexto(req("/object/" + bucket).header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(paraJson(Map.of("prefixes", new String[] { caminho })))));
        } catch (RuntimeException e) {
            log.warn("Não foi possível apagar {}/{} do Supabase Storage: {}", bucket, caminho, e.getMessage());
        }
    }

    @Override
    public void copiar(String bucket, String origem, String destino) {
        String corpo = paraJson(Map.of("bucketId", bucket, "sourceKey", origem, "destinationKey", destino));
        HttpResponse<String> r = enviarTexto(req("/object/copy").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)));
        if (r.statusCode() >= 300) {
            throw new UncheckedIOException(new IOException("Falha ao copiar o arquivo no Supabase Storage (HTTP "
                    + r.statusCode() + "): " + r.body()));
        }
    }

    @Override
    public String urlPublica(String bucket, String caminho) {
        return base + "/object/public/" + bucket + "/" + codificar(caminho);
    }

    private HttpRequest.Builder req(String caminho) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + caminho)).timeout(Duration.ofSeconds(60))
                .header("apikey", chave);
        // chaves novas (sb_secret_...) vão só no cabeçalho apikey; a chave antiga service_role (JWT) vai nos dois
        if (!chave.startsWith("sb_")) {
            b.header("Authorization", "Bearer " + chave);
        }
        return b;
    }

    private HttpResponse<String> enviarTexto(HttpRequest.Builder b) {
        try {
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("Sem conexão com o Supabase Storage (" + base + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String paraJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String codificar(String caminho) {
        return URLEncoder.encode(caminho, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }
}
