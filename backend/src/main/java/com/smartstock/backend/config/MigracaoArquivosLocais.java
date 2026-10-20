package com.smartstock.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.smartstock.backend.model.Mensagem;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.ProjetoAnexo;
import com.smartstock.backend.repository.MensagemRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.ProjetoAnexoRepository;
import com.smartstock.backend.storage.Armazenamento;
import com.smartstock.backend.storage.TipoArquivo;

/**
 * Leva para o Supabase Storage os arquivos que ficavam nas pastas do computador (versões anteriores do sistema):
 * backend/uploads/pastilhas, backend/uploads-privado/mensagens e backend/uploads-privado/projetos.
 * Cada arquivo só é apagado do computador depois de enviado com sucesso. Roda a cada início, mas só faz algo
 * enquanto ainda houver arquivos antigos.
 */
@Component
@Order(0)
public class MigracaoArquivosLocais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoArquivosLocais.class);

    private static final Path FOTOS = Path.of("uploads", "pastilhas");
    private static final Path MENSAGENS = Path.of("uploads-privado", "mensagens");
    private static final Path PROJETOS = Path.of("uploads-privado", "projetos");
    private static final String URL_ANTIGA = "/uploads/pastilhas/";

    private final PastilhaRepository pastilhaRepository;
    private final MensagemRepository mensagemRepository;
    private final ProjetoAnexoRepository anexoRepository;
    private final Armazenamento armazenamento;

    public MigracaoArquivosLocais(PastilhaRepository pastilhaRepository, MensagemRepository mensagemRepository,
            ProjetoAnexoRepository anexoRepository, Armazenamento armazenamento) {
        this.pastilhaRepository = pastilhaRepository;
        this.mensagemRepository = mensagemRepository;
        this.anexoRepository = anexoRepository;
        this.armazenamento = armazenamento;
    }

    @Override
    public void run(String... args) {
        int fotos = 0;
        for (Pastilha p : pastilhaRepository.findAll()) {
            if (p.getImagemUrl() != null && p.getImagemUrl().startsWith(URL_ANTIGA)) {
                if (migrarFoto(p)) {
                    fotos++;
                }
            }
        }
        int imagens = 0;
        for (Mensagem m : mensagemRepository.findByImagemArquivoIsNotNull()) {
            if (enviarSeExistir(MENSAGENS.resolve(m.getImagemArquivo()), Armazenamento.MENSAGENS, m.getImagemArquivo(),
                    m.getImagemTipo())) {
                imagens++;
            }
        }
        int anexos = 0;
        for (ProjetoAnexo a : anexoRepository.findAll()) {
            if (enviarSeExistir(PROJETOS.resolve(a.getArquivo()), Armazenamento.PROJETOS, a.getArquivo(), a.getTipo())) {
                anexos++;
            }
        }
        if (fotos + imagens + anexos > 0) {
            log.info("Arquivos levados para o Supabase Storage: {} foto(s) de itens, {} imagem(ns) de mensagens, "
                    + "{} anexo(s) de projetos.", fotos, imagens, anexos);
        }
        apagarPastaSeVazia(FOTOS);
        apagarPastaSeVazia(Path.of("uploads"));
        apagarPastaSeVazia(MENSAGENS);
        apagarPastaSeVazia(PROJETOS);
        apagarPastaSeVazia(Path.of("uploads-privado"));
    }

    private boolean migrarFoto(Pastilha p) {
        String nome = Armazenamento.nomeNaUrl(p.getImagemUrl());
        Path local = FOTOS.resolve(nome);
        byte[] conteudo = ler(local);
        if (conteudo == null && nome.startsWith("seed-")) {
            // foto dos dados de exemplo: vem de dentro do próprio sistema
            conteudo = recurso("/seed-images/" + nome.substring("seed-".length()));
        }
        if (conteudo == null) {
            log.warn("Foto do item {} não encontrada no computador ({}); o item fica sem foto.", p.getCodigo(), local);
            p.setImagemUrl(null);
            pastilhaRepository.save(p);
            return false;
        }
        armazenamento.enviar(Armazenamento.PASTILHAS, nome, conteudo, TipoArquivo.peloNome(nome));
        p.setImagemUrl(armazenamento.urlPublica(Armazenamento.PASTILHAS, nome));
        pastilhaRepository.save(p);
        apagarLocal(local);
        return true;
    }

    private boolean enviarSeExistir(Path local, String bucket, String nome, String tipo) {
        byte[] conteudo = ler(local);
        if (conteudo == null) {
            return false;
        }
        armazenamento.enviar(bucket, nome, conteudo, tipo);
        apagarLocal(local);
        return true;
    }

    private static byte[] ler(Path p) {
        try {
            return Files.isRegularFile(p) ? Files.readAllBytes(p) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] recurso(String caminho) {
        try (InputStream in = getClass().getResourceAsStream(caminho)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static void apagarLocal(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Arquivo enviado ao Supabase, mas não foi possível apagá-lo do computador: {}", p);
        }
    }

    private static void apagarPastaSeVazia(Path pasta) {
        try {
            if (Files.isDirectory(pasta)) {
                try (var itens = Files.list(pasta)) {
                    if (itens.findAny().isEmpty()) {
                        Files.delete(pasta);
                    }
                }
            }
        } catch (IOException ignored) {
            // melhor esforço
        }
    }
}
