package com.smartstock.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.dto.PastilhaRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.repository.FornecedorProdutoRepository;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.PastilhaRepository;

@Service
public class PastilhaService {

    private static final List<String> TIPOS_PERMITIDOS = List.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final PastilhaRepository pastilhaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    private final ModeloProjetoService modeloProjetoService;

    public PastilhaService(PastilhaRepository pastilhaRepository, FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository, ModeloProjetoService modeloProjetoService) {
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.modeloProjetoService = modeloProjetoService;
    }

    public List<Pastilha> listar() {
        return pastilhaRepository.findAll();
    }

    public Pastilha buscarPorId(Long id) {
        return pastilhaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pastilha não encontrada: " + id));
    }

    public List<Pastilha> listarAlertas() {
        return pastilhaRepository.findAlertasEstoqueMinimo();
    }

    public Pastilha criar(PastilhaRequest request) {
        if (pastilhaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe uma pastilha cadastrada com o código " + request.getCodigo());
        }

        Pastilha pastilha = new Pastilha();
        aplicar(pastilha, request);
        pastilha.setQuantidadeAtual(request.getQuantidadeAtual() != null ? request.getQuantidadeAtual() : 0);
        return pastilhaRepository.save(pastilha);
    }

    public Pastilha atualizar(Long id, PastilhaRequest request) {
        Pastilha pastilha = buscarPorId(id);

        if (!pastilha.getCodigo().equals(request.getCodigo()) && pastilhaRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe uma pastilha cadastrada com o código " + request.getCodigo());
        }

        aplicar(pastilha, request);
        return pastilhaRepository.save(pastilha);
    }

    @Transactional
    public void excluir(Long id) {
        Pastilha pastilha = buscarPorId(id);
        fornecedorProdutoRepository.deleteByPastilhaId(id);
        modeloProjetoService.removerPastilhaDosModelos(id);
        removerArquivoImagem(pastilha);
        pastilhaRepository.delete(pastilha);
    }

    public Pastilha salvarImagem(Long id, MultipartFile arquivo) {
        Pastilha pastilha = buscarPorId(id);

        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Selecione um arquivo de imagem.");
        }
        if (!TIPOS_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new BusinessException("Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.");
        }

        removerArquivoImagem(pastilha);

        try {
            Path diretorio = Path.of(uploadsDir, "pastilhas");
            Files.createDirectories(diretorio);

            String extensao = extensaoDoArquivo(arquivo.getOriginalFilename());
            String nomeArquivo = UUID.randomUUID() + extensao;
            Path destino = diretorio.resolve(nomeArquivo);
            Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            pastilha.setImagemUrl("/uploads/pastilhas/" + nomeArquivo);
            return pastilhaRepository.save(pastilha);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a imagem da pastilha.", e);
        }
    }

    public Pastilha removerImagem(Long id) {
        Pastilha pastilha = buscarPorId(id);
        removerArquivoImagem(pastilha);
        pastilha.setImagemUrl(null);
        return pastilhaRepository.save(pastilha);
    }

    private void removerArquivoImagem(Pastilha pastilha) {
        if (pastilha.getImagemUrl() == null) {
            return;
        }
        try {
            String nomeArquivo = pastilha.getImagemUrl().substring(pastilha.getImagemUrl().lastIndexOf('/') + 1);
            Path arquivo = Path.of(uploadsDir, "pastilhas", nomeArquivo);
            Files.deleteIfExists(arquivo);
        } catch (IOException ignored) {
            // melhor esforço: não impede a operação principal caso o arquivo não possa ser removido
        }
    }

    private String extensaoDoArquivo(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf('.'));
    }

    private void aplicar(Pastilha pastilha, PastilhaRequest request) {
        pastilha.setCodigo(request.getCodigo());
        pastilha.setDescricao(request.getDescricao());
        pastilha.setEstoqueMinimo(request.getEstoqueMinimo());

        if (request.getFabricanteId() != null) {
            Fornecedor fabricante = fornecedorRepository.findById(request.getFabricanteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fabricante não encontrado: " + request.getFabricanteId()));
            pastilha.setFabricante(fabricante);
        } else {
            pastilha.setFabricante(null);
        }
    }
}
