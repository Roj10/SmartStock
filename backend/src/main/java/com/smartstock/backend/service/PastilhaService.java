package com.smartstock.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

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
import com.smartstock.backend.storage.Armazenamento;
import com.smartstock.backend.storage.TipoArquivo;

@Service
public class PastilhaService {

    private final PastilhaRepository pastilhaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;

    private final Armazenamento armazenamento;

    private final ModeloProjetoService modeloProjetoService;

    public PastilhaService(PastilhaRepository pastilhaRepository, FornecedorRepository fornecedorRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository, ModeloProjetoService modeloProjetoService,
            Armazenamento armazenamento) {
        this.pastilhaRepository = pastilhaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.modeloProjetoService = modeloProjetoService;
        this.armazenamento = armazenamento;
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
        if (arquivo.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException("A imagem é grande demais. O limite é de 5 MB.");
        }
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a imagem enviada.", e);
        }
        // o tipo vem do conteúdo do arquivo, não do nome nem do que o navegador informou
        String tipo = TipoArquivo.detectar(conteudo);
        if (!TipoArquivo.ehImagem(tipo)) {
            throw new BusinessException("Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.");
        }

        // fotos dos itens ficam no bucket público "pastilhas" do Supabase Storage
        String nomeArquivo = UUID.randomUUID() + TipoArquivo.extensao(tipo);
        armazenamento.enviar(Armazenamento.PASTILHAS, nomeArquivo, conteudo, tipo);
        removerArquivoImagem(pastilha);
        pastilha.setImagemUrl(armazenamento.urlPublica(Armazenamento.PASTILHAS, nomeArquivo));
        return pastilhaRepository.save(pastilha);
    }

    public Pastilha removerImagem(Long id) {
        Pastilha pastilha = buscarPorId(id);
        removerArquivoImagem(pastilha);
        pastilha.setImagemUrl(null);
        return pastilhaRepository.save(pastilha);
    }

    private void removerArquivoImagem(Pastilha pastilha) {
        if (pastilha.getImagemUrl() != null) {
            armazenamento.apagar(Armazenamento.PASTILHAS, Armazenamento.nomeNaUrl(pastilha.getImagemUrl()));
        }
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
