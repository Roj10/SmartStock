package com.smartstock.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoAnexo;
import com.smartstock.backend.repository.ProjetoAnexoRepository;
import com.smartstock.backend.storage.Armazenamento;
import com.smartstock.backend.storage.TipoArquivo;

/**
 * Arquivos anexados aos projetos do Progresso (PDF, desenhos de CAD, fotos, planilhas...).
 * Ficam no bucket privado "projetos" do Supabase Storage e só são entregues pela API, nunca por link público.
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
    private final Armazenamento armazenamento;

    public AnexoProjetoService(ProjetoAnexoRepository anexoRepository, Armazenamento armazenamento) {
        this.anexoRepository = anexoRepository;
        this.armazenamento = armazenamento;
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

    /** Envia os arquivos para o Storage e os liga ao projeto (que precisa estar salvo). */
    public void anexar(Projeto projeto, List<MultipartFile> arquivos) {
        validar(projeto, arquivos);
        List<String> enviados = new ArrayList<>();
        try {
            for (MultipartFile arquivo : naoVazios(arquivos)) {
                String nome = nomeLimpo(arquivo.getOriginalFilename());
                byte[] conteudo = arquivo.getBytes();
                String tipo = TipoArquivo.detectar(conteudo);
                if (extensao(nome).equals("pdf") && !tipo.equals("application/pdf")) {
                    throw new BusinessException("O arquivo \"" + nome + "\" não é um PDF válido.");
                }
                String guardado = UUID.randomUUID() + "." + extensao(nome);
                armazenamento.enviar(Armazenamento.PROJETOS, guardado, conteudo, tipo);
                enviados.add(guardado);
                adicionar(projeto, nome, guardado, tipo, arquivo.getSize());
            }
        } catch (IOException e) {
            apagar(enviados);
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        } catch (RuntimeException e) {
            apagar(enviados);
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
            armazenamento.copiar(Armazenamento.PROJETOS, origem.getArquivo(), novoNome);
            adicionar(destino, origem.getNomeOriginal(), novoNome, origem.getTipo(), origem.getTamanho());
        }
    }

    public ArquivoAnexo abrir(Long projetoId, Long anexoId) {
        ProjetoAnexo anexo = buscar(projetoId, anexoId);
        byte[] conteudo = armazenamento.baixar(Armazenamento.PROJETOS, anexo.getArquivo());
        return new ArquivoAnexo(new ByteArrayResource(conteudo), anexo.getNomeOriginal(), anexo.getTipo(),
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

    private void apagarArquivo(String arquivo) {
        armazenamento.apagar(Armazenamento.PROJETOS, arquivo);
    }

    private void apagar(List<String> arquivos) {
        arquivos.forEach(this::apagarArquivo);
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

    private static boolean podeAbrirNoNavegador(String tipo) {
        return tipo.equals("application/pdf") || tipo.startsWith("image/");
    }

    public record ArquivoAnexo(Resource recurso, String nome, String tipo, boolean abrirNoNavegador) {
    }
}
