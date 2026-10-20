package com.smartstock.backend.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.smartstock.backend.exception.ResourceNotFoundException;

/**
 * Arquivos numa pasta do computador. Usado só no perfil "h2" (sem internet / testes);
 * as fotos dos itens são servidas pelo próprio backend em /arquivos/pastilhas/...
 */
@Component
@ConditionalOnProperty(name = "app.armazenamento", havingValue = "local")
public class LocalStorage implements Armazenamento {

    private final Path raiz;

    public LocalStorage(@Value("${app.armazenamento.local-dir:armazenamento-local}") String dir) {
        this.raiz = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public void enviar(String bucket, String caminho, byte[] conteudo, String tipo) {
        try {
            Path destino = arquivo(bucket, caminho);
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar o arquivo.", e);
        }
    }

    @Override
    public byte[] baixar(String bucket, String caminho) {
        Path p = arquivo(bucket, caminho);
        if (!Files.isReadable(p)) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void apagar(String bucket, String caminho) {
        try {
            Files.deleteIfExists(arquivo(bucket, caminho));
        } catch (IOException | RuntimeException ignored) {
            // melhor esforço
        }
    }

    @Override
    public void copiar(String bucket, String origem, String destino) {
        try {
            Files.copy(arquivo(bucket, origem), arquivo(bucket, destino), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao copiar o arquivo.", e);
        }
    }

    @Override
    public String urlPublica(String bucket, String caminho) {
        return "/arquivos/" + bucket + "/" + caminho;
    }

    public Path raiz() {
        return raiz;
    }

    private Path arquivo(String bucket, String caminho) {
        Path p = raiz.resolve(bucket).resolve(caminho).normalize();
        if (!p.startsWith(raiz.resolve(bucket))) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        return p;
    }
}
