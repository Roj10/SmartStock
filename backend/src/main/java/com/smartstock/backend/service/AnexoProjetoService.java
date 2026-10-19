package com.smartstock.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoAnexo;
import com.smartstock.backend.repository.ProjetoAnexoRepository;

/**
 * Arquivos anexados aos projetos do Progresso (PDF, desenhos de CAD, fotos, planilhas...).
 * Ficam numa pasta privada e só são entregues pela API, nunca por link público.
 */
@Service
public class AnexoProjetoService {

    public static final long TAMANHO_MAXIMO = 20L * 1024 * 1024;
    public static final int MAXIMO_POR_PROJETO = 20;

    /** Extensões aceitas: documentos, desenhos/modelos de CAD, imagens e arquivos compactados. */
    public static final Set<String> EXTENSOES = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "gif",
            "dwg", "dxf", "dwf", "step", "stp", "iges", "igs", "stl", "skp", "3dm",
            "ipt", "iam", "idw", "sldprt", "sldasm", "slddrw",
            "doc", "docx", "xls", "xlsx", "odt", "ods", "txt", "csv",
            "zip", "rar", "7z");

    private final ProjetoAnexoRepository anexoRepository;

    @Value("${app.projetos.dir}")
    private String projetosDir;

    public AnexoProjetoService(ProjetoAnexoRepository anexoRepository) {
        this.anexoRepository = anexoRepository;
    }

    /** Confere todos os arquivos antes de gravar qualquer coisa: se um for inválido, nenhum é aceito. */
    public void validar(Projeto projeto, List<MultipartFile> arquivos) {
        List<MultipartFile> lista = naoVazios(arquivos);
        if (projeto.getAnexos().size() + lista.size() > MAXIMO_POR_PROJETO) {
            throw new BusinessException("Cada projeto pode ter no máximo " + MAXIMO_POR_PROJETO + " arquivos anexados.");
        }
        for (MultipartFile arquivo : lista) {
            String nome = nomeLimpo(arquivo.getOriginalFilename());
            if (!EXTENSOES.contains(extensao(nome))) {
                throw new BusinessException("O arquivo \"" + nome + "\" não é de um tipo aceito. Use PDF, imagens, "
                        + "desenhos de CAD (DWG, DXF, STEP, STL...), documentos, planilhas ou ZIP.");
            }
            if (arquivo.getSize() > TAMANHO_MAXIMO) {
                throw new BusinessException("O arquivo \"" + nome + "\" passa do limite de 20 MB.");
            }
        }
    }

    /** Grava os arquivos na pasta privada e os liga ao projeto (que precisa estar salvo). */
    public void anexar(Projeto projeto, List<MultipartFile> arquivos) {
        validar(projeto, arquivos);
        List<Path> gravados = new ArrayList<>();
        try {
            for (MultipartFile arquivo : naoVazios(arquivos)) {
                String nome = nomeLimpo(arquivo.getOriginalFilename());
                String tipo;
                try (InputStream in = arquivo.getInputStream()) {
                    tipo = tipoPeloConteudo(in.readNBytes(12));
                }
                if (extensao(nome).equals("pdf") && !tipo.equals("application/pdf")) {
                    throw new BusinessException("O arquivo \"" + nome + "\" não é um PDF válido.");
                }
                Path destino = gravar(arquivo, extensao(nome));
                gravados.add(destino);
                adicionar(projeto, nome, destino.getFileName().toString(), tipo, arquivo.getSize());
            }
        } catch (IOException e) {
            apagar(gravados);
            throw new UncheckedIOException("Falha ao salvar o arquivo do projeto.", e);
        } catch (RuntimeException e) {
            apagar(gravados);
            throw e;
        }
    }

    /** Ao repetir um projeto, copia os arquivos escolhidos do projeto de origem (cada cópia é independente). */
    public void copiar(Projeto destino, List<Long> anexoIds) {
        if (anexoIds == null || anexoIds.isEmpty()) {
            return;
        }
        if (destino.getAnexos().size() + anexoIds.size() > MAXIMO_POR_PROJETO) {
            throw new BusinessException("Cada projeto pode ter no máximo " + MAXIMO_POR_PROJETO + " arquivos anexados.");
        }
        for (Long id : anexoIds) {
            ProjetoAnexo origem = anexoRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado: " + id));
            String novoNome = UUID.randomUUID() + "." + extensao(origem.getNomeOriginal());
            try {
                Files.createDirectories(pasta());
                Files.copy(caminho(origem.getArquivo()), pasta().resolve(novoNome));
            } catch (IOException e) {
                throw new UncheckedIOException("Falha ao copiar o arquivo \"" + origem.getNomeOriginal() + "\".", e);
            }
            adicionar(destino, origem.getNomeOriginal(), novoNome, origem.getTipo(), origem.getTamanho());
        }
    }

    public ArquivoAnexo abrir(Long projetoId, Long anexoId) {
        ProjetoAnexo anexo = buscar(projetoId, anexoId);
        Path arquivo = caminho(anexo.getArquivo());
        if (!Files.isReadable(arquivo)) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        return new ArquivoAnexo(new FileSystemResource(arquivo), anexo.getNomeOriginal(), anexo.getTipo(),
                podeAbrirNoNavegador(anexo.getTipo()));
    }

    public void remover(Projeto projeto, Long anexoId) {
        ProjetoAnexo anexo = buscar(projeto.getId(), anexoId);
        projeto.getAnexos().remove(anexo);
        apagarArquivo(anexo.getArquivo());
    }

    /** Apaga do disco os arquivos de um projeto que está sendo excluído. */
    public void apagarArquivosDoProjeto(Projeto projeto) {
        projeto.getAnexos().forEach(a -> apagarArquivo(a.getArquivo()));
    }

    private ProjetoAnexo buscar(Long projetoId, Long anexoId) {
        return anexoRepository.findByIdAndProjetoId(anexoId, projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado."));
    }

    private void adicionar(Projeto projeto, String nome, String arquivo, String tipo, long tamanho) {
        ProjetoAnexo anexo = new ProjetoAnexo();
        anexo.setProjeto(projeto);
        anexo.setNomeOriginal(nome);
        anexo.setArquivo(arquivo);
        anexo.setTipo(tipo);
        anexo.setTamanho(tamanho);
        projeto.getAnexos().add(anexoRepository.save(anexo));
    }

    private Path gravar(MultipartFile arquivo, String extensao) throws IOException {
        Files.createDirectories(pasta());
        Path destino = pasta().resolve(UUID.randomUUID() + "." + extensao);
        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, destino);
        }
        return destino;
    }

    private Path pasta() {
        return Path.of(projetosDir).toAbsolutePath().normalize();
    }

    private Path caminho(String arquivo) {
        Path p = pasta().resolve(arquivo).normalize();
        if (!p.startsWith(pasta())) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        }
        return p;
    }

    private void apagarArquivo(String arquivo) {
        try {
            Files.deleteIfExists(caminho(arquivo));
        } catch (IOException | RuntimeException ignored) {
            // melhor esforço: não impede a operação principal
        }
    }

    private void apagar(List<Path> arquivos) {
        for (Path p : arquivos) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                // melhor esforço
            }
        }
    }

    private static List<MultipartFile> naoVazios(List<MultipartFile> arquivos) {
        return arquivos == null ? List.of() : arquivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
    }

    /** Só o nome do arquivo (sem pastas), sem caracteres de controle e com tamanho limitado. */
    static String nomeLimpo(String original) {
        String nome = original == null ? "" : original.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nome.isEmpty()) {
            nome = "arquivo";
        }
        if (nome.length() > 150) {
            String ext = extensao(nome);
            nome = nome.substring(0, 140 - ext.length()) + "." + ext;
        }
        return nome;
    }

    static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * O tipo é decidido pelo conteúdo, não pelo nome: só PDF e imagens verdadeiras abrem no navegador;
     * todo o resto é entregue como download.
     */
    static String tipoPeloConteudo(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W'
                && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private static boolean podeAbrirNoNavegador(String tipo) {
        return tipo.equals("application/pdf") || tipo.startsWith("image/");
    }

    public record ArquivoAnexo(Resource recurso, String nome, String tipo, boolean abrirNoNavegador) {
    }
}
