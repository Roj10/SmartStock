package com.smartstock.backend.config;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.TipoFornecedor;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.UsuarioRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PastilhaRepository pastilhaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public DataSeeder(UsuarioRepository usuarioRepository, FornecedorRepository fornecedorRepository,
            PastilhaRepository pastilhaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.pastilhaRepository = pastilhaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedFornecedores();
        seedPastilhas();
    }

    private void seedUsuarios() {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setUsername("admin");
        admin.setSenha(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ADMIN);
        admin.setPermissoes(Set.of(Modulo.values()));
        usuarioRepository.save(admin);

        // Exemplo de conta com acesso restrito a estoque e movimentações (produção),
        // sem acesso a Fornecedores nem à administração de Usuários.
        Usuario operador = new Usuario();
        operador.setNome("Operador de Estoque");
        operador.setUsername("operador");
        operador.setSenha(passwordEncoder.encode("operador123"));
        operador.setRole(Role.OPERADOR);
        operador.setPermissoes(Set.of(Modulo.PASTILHAS, Modulo.MOVIMENTACOES));
        usuarioRepository.save(operador);
    }

    private void seedFornecedores() {
        if (fornecedorRepository.count() > 0) {
            return;
        }

        Fornecedor sandvik = new Fornecedor();
        sandvik.setNome("Sandvik Coromant");
        sandvik.setTipo(TipoFornecedor.FABRICANTE);
        sandvik.setContato("Com. Industrial");
        sandvik.setTelefone("(47) 3333-1111");
        sandvik.setEmail("contato@sandvik.example.com");
        fornecedorRepository.save(sandvik);

        Fornecedor mitsubishi = new Fornecedor();
        mitsubishi.setNome("Mitsubishi Materials");
        mitsubishi.setTipo(TipoFornecedor.FABRICANTE);
        mitsubishi.setContato("Suporte Técnico");
        mitsubishi.setTelefone("(47) 3333-2222");
        mitsubishi.setEmail("contato@mitsubishi.example.com");
        fornecedorRepository.save(mitsubishi);

        Fornecedor distribuidora = new Fornecedor();
        distribuidora.setNome("Distribuidora SC Ferramentas");
        distribuidora.setTipo(TipoFornecedor.FORNECEDOR);
        distribuidora.setContato("Vendas");
        distribuidora.setTelefone("(49) 3222-4444");
        distribuidora.setEmail("vendas@scferramentas.example.com");
        fornecedorRepository.save(distribuidora);
    }

    private void seedPastilhas() {
        if (pastilhaRepository.count() > 0) {
            return;
        }

        Fornecedor sandvik = fornecedorRepository.findAll().get(0);
        Fornecedor mitsubishi = fornecedorRepository.findAll().get(1);

        Pastilha p1 = new Pastilha();
        p1.setCodigo("CNMG120408");
        p1.setDescricao("Pastilha de torneamento CNMG 120408");
        p1.setFabricante(sandvik);
        p1.setEstoqueMinimo(20);
        p1.setQuantidadeAtual(35);
        p1.setImagemUrl(gerarImagemExemplo("CNMG120408", new Color(31, 78, 121)));
        pastilhaRepository.save(p1);

        Pastilha p2 = new Pastilha();
        p2.setCodigo("APMT160408");
        p2.setDescricao("Pastilha de fresamento APMT 160408");
        p2.setFabricante(mitsubishi);
        p2.setEstoqueMinimo(15);
        p2.setQuantidadeAtual(8);
        p2.setImagemUrl(gerarImagemExemplo("APMT160408", new Color(27, 138, 90)));
        pastilhaRepository.save(p2);

        Pastilha p3 = new Pastilha();
        p3.setCodigo("DCMT11T304");
        p3.setDescricao("Pastilha de acabamento DCMT 11T304");
        p3.setFabricante(sandvik);
        p3.setEstoqueMinimo(10);
        p3.setQuantidadeAtual(10);
        pastilhaRepository.save(p3);
    }

    /**
     * Gera uma imagem de exemplo (placeholder) para demonstrar a galeria de
     * imagens sem depender de um arquivo binário versionado no repositório.
     */
    private String gerarImagemExemplo(String codigo, Color cor) {
        int tamanho = 300;
        BufferedImage imagem = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(cor);
        g.fillRect(0, 0, tamanho, tamanho);
        g.setColor(cor.darker());
        g.fillOval(tamanho / 2 - 70, tamanho / 2 - 70, 140, 140);
        g.setColor(Color.WHITE);
        g.fillOval(tamanho / 2 - 25, tamanho / 2 - 25, 50, 50);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(codigo, 20, tamanho - 24);
        g.dispose();

        try {
            Path diretorio = Path.of(uploadsDir, "pastilhas");
            Files.createDirectories(diretorio);
            String nomeArquivo = "seed-" + codigo.toLowerCase() + ".png";
            Path destino = diretorio.resolve(nomeArquivo);
            ImageIO.write(imagem, "png", destino.toFile());
            return "/uploads/pastilhas/" + nomeArquivo;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar imagem de exemplo.", e);
        }
    }
}
