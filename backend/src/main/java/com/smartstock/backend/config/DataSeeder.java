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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartstock.backend.model.ChecklistItem;
import com.smartstock.backend.model.Fornecedor;
import com.smartstock.backend.model.FornecedorProduto;
import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Pastilha;
import com.smartstock.backend.model.Projeto;
import com.smartstock.backend.model.ProjetoMaterial;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.StatusProjeto;
import com.smartstock.backend.model.StatusVenda;
import com.smartstock.backend.model.TipoFornecedor;
import com.smartstock.backend.model.Mensagem;
import com.smartstock.backend.model.ModeloProjeto;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.model.Venda;
import com.smartstock.backend.repository.FornecedorProdutoRepository;
import com.smartstock.backend.repository.FornecedorRepository;
import com.smartstock.backend.repository.PastilhaRepository;
import com.smartstock.backend.repository.ProjetoRepository;
import com.smartstock.backend.repository.MensagemRepository;
import com.smartstock.backend.repository.ModeloProjetoRepository;
import com.smartstock.backend.repository.UsuarioRepository;
import com.smartstock.backend.repository.VendaRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final MensagemRepository mensagemRepository;
    private final ModeloProjetoRepository modeloProjetoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final PastilhaRepository pastilhaRepository;
    private final ProjetoRepository projetoRepository;
    private final FornecedorProdutoRepository fornecedorProdutoRepository;
    private final VendaRepository vendaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public DataSeeder(UsuarioRepository usuarioRepository, FornecedorRepository fornecedorRepository,
            PastilhaRepository pastilhaRepository, ProjetoRepository projetoRepository,
            FornecedorProdutoRepository fornecedorProdutoRepository, VendaRepository vendaRepository,
            MensagemRepository mensagemRepository, ModeloProjetoRepository modeloProjetoRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.pastilhaRepository = pastilhaRepository;
        this.projetoRepository = projetoRepository;
        this.fornecedorProdutoRepository = fornecedorProdutoRepository;
        this.vendaRepository = vendaRepository;
        this.mensagemRepository = mensagemRepository;
        this.modeloProjetoRepository = modeloProjetoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedFornecedores();
        seedPastilhas();
        seedProjetos();
        seedFinanceiro();
        seedMensagens();
        seedModelos();
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

        // Exemplo de conta com acesso restrito a estoque, movimentações e progresso
        // de produção, sem acesso a Fornecedores nem à administração de Usuários.
        Usuario operador = new Usuario();
        operador.setNome("Operador de Estoque");
        operador.setUsername("operador");
        operador.setSenha(passwordEncoder.encode("operador123"));
        operador.setRole(Role.OPERADOR);
        operador.setPermissoes(Set.of(Modulo.PASTILHAS, Modulo.MOVIMENTACOES, Modulo.PROJETOS));
        usuarioRepository.save(operador);

        // Conta do setor financeiro: compras, valores, vendas e planos.
        Usuario financeiro = new Usuario();
        financeiro.setNome("Setor Financeiro");
        financeiro.setUsername("financeiro");
        financeiro.setSenha(passwordEncoder.encode("financeiro123"));
        financeiro.setRole(Role.OPERADOR);
        financeiro.setPermissoes(Set.of(Modulo.FINANCEIRO, Modulo.FORNECEDORES));
        usuarioRepository.save(financeiro);
    }

    /** Conversa de exemplo: a produção pede ao financeiro a compra de mais peças para o estoque. */
    private void seedMensagens() {
        if (mensagemRepository.count() > 0) {
            return;
        }
        Usuario operador = usuarioRepository.findByUsername("operador").orElse(null);
        Usuario financeiro = usuarioRepository.findByUsername("financeiro").orElse(null);
        if (operador == null || financeiro == null) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        mensagem(operador, financeiro, "Bom dia! O estoque de DCMT está ficando baixo para a próxima ordem de produção. "
                + "Dá para comprar mais 50 unidades?", agora.minusMinutes(40), true);
        mensagem(financeiro, operador, "Bom dia! Vou consultar os fornecedores e te aviso o prazo de entrega.",
                agora.minusMinutes(25), true);
        mensagem(financeiro, operador,
                "Consegui com a Distribuidora: chega na quinta. Pode dar a entrada no sistema quando receber.",
                agora.minusMinutes(5), false);
    }

    private void mensagem(Usuario de, Usuario para, String texto, LocalDateTime dataHora, boolean lida) {
        Mensagem m = new Mensagem();
        m.setRemetente(de);
        m.setDestinatario(para);
        m.setTexto(texto);
        m.setDataHora(dataHora);
        m.setLida(lida);
        mensagemRepository.save(m);
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

    private void seedProjetos() {
        if (projetoRepository.count() > 0) {
            return;
        }

        List<Pastilha> pastilhas = pastilhaRepository.findAll();
        Pastilha cnmg = pastilhas.get(0);
        Pastilha apmt = pastilhas.get(1);
        Pastilha dcmt = pastilhas.get(2);

        // Exemplo citado pelo usuário: projeto de cerca de metal, em produção,
        // com parte do checklist já concluída.
        Projeto cercaMetal = new Projeto();
        cercaMetal.setNome("Cerca de metal - Cliente ABC");
        cercaMetal.setDescricao("Cerca de metal sob medida para o pátio do cliente.");
        cercaMetal.setStatus(StatusProjeto.EM_PRODUCAO);
        cercaMetal.setDataCriacao(LocalDateTime.now().minusDays(3));
        cercaMetal.setDataInicioProducao(LocalDateTime.now().minusDays(2));
        adicionarMaterial(cercaMetal, cnmg, 10);
        adicionarChecklist(cercaMetal,
                new String[] { "Separar as pastilhas e materiais da lista", "Fazer o ligamento (solda) das peças",
                        "Fazer as rodas", "Pintura", "Finalização e conferência" },
                2);
        projetoRepository.save(cercaMetal);

        Projeto suporteIndustrial = new Projeto();
        suporteIndustrial.setNome("Suporte industrial - Cliente XYZ");
        suporteIndustrial.setDescricao("Suporte metálico para fixação de equipamento.");
        suporteIndustrial.setStatus(StatusProjeto.AGUARDANDO);
        suporteIndustrial.setDataCriacao(LocalDateTime.now().minusHours(6));
        adicionarMaterial(suporteIndustrial, apmt, 4);
        adicionarChecklist(suporteIndustrial,
                new String[] { "Separar materiais", "Corte e furação", "Montagem", "Acabamento" }, 0);
        projetoRepository.save(suporteIndustrial);

        Projeto portao = new Projeto();
        portao.setNome("Portão basculante - Cliente JKL");
        portao.setDescricao("Portão basculante pronto, aguardando a retirada.");
        portao.setStatus(StatusProjeto.EM_ESTOQUE);
        portao.setDataCriacao(LocalDateTime.now().minusDays(7));
        portao.setDataInicioProducao(LocalDateTime.now().minusDays(6));
        portao.setDataFinalizacaoProducao(LocalDateTime.now().minusDays(2));
        adicionarChecklist(portao, new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(portao);

        Projeto escada = new Projeto();
        escada.setNome("Escada marinheiro - Cliente MNO");
        escada.setDescricao("Escada de acesso ao reservatório, liberada para entrega.");
        escada.setStatus(StatusProjeto.PRONTO_ENTREGA);
        escada.setCliente("Cliente MNO");
        adicionarMaterial(escada, dcmt, 2);
        escada.setDataCriacao(LocalDateTime.now().minusDays(8));
        escada.setDataInicioProducao(LocalDateTime.now().minusDays(7));
        escada.setDataFinalizacaoProducao(LocalDateTime.now().minusDays(3));
        adicionarChecklist(escada, new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(escada);

        Projeto baseEsteira = new Projeto();
        baseEsteira.setNome("Base para esteira - Cliente DEF");
        baseEsteira.setDescricao("Base de sustentação para esteira transportadora.");
        baseEsteira.setStatus(StatusProjeto.PEDIDO_ENVIADO);
        baseEsteira.setCliente("Cliente DEF");
        baseEsteira.setDataCriacao(LocalDateTime.now().minusDays(10));
        baseEsteira.setDataInicioProducao(LocalDateTime.now().minusDays(9));
        baseEsteira.setDataFinalizacaoProducao(LocalDateTime.now().minusDays(1));
        baseEsteira.setDataPedido(LocalDate.now().minusDays(10));
        baseEsteira.setMetaEntrega(LocalDate.now().plusDays(5));
        adicionarMaterial(baseEsteira, cnmg, 20);
        adicionarChecklist(baseEsteira,
                new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(baseEsteira);

        Projeto gradeProtecao = new Projeto();
        gradeProtecao.setNome("Grade de proteção - Cliente GHI");
        gradeProtecao.setDescricao("Grade de proteção para máquina industrial.");
        gradeProtecao.setStatus(StatusProjeto.ENTREGUE);
        gradeProtecao.setCliente("Cliente GHI");
        gradeProtecao.setDataCriacao(LocalDateTime.now().minusDays(20));
        gradeProtecao.setDataInicioProducao(LocalDateTime.now().minusDays(19));
        gradeProtecao.setDataFinalizacaoProducao(LocalDateTime.now().minusDays(15));
        gradeProtecao.setDataPedido(LocalDate.now().minusDays(20));
        gradeProtecao.setMetaEntrega(LocalDate.now().minusDays(13));
        gradeProtecao.setDataEntrega(LocalDateTime.now().minusDays(14));
        adicionarMaterial(gradeProtecao, apmt, 5);
        adicionarChecklist(gradeProtecao,
                new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(gradeProtecao);
    }

    /** Modelos (padrões) de projeto de exemplo, para criar novos pedidos do mesmo tipo rapidamente. */
    private void seedModelos() {
        if (modeloProjetoRepository.count() > 0) {
            return;
        }
        Long cnmg = pastilhaRepository.findAll().stream().filter(p -> "CNMG120408".equals(p.getCodigo()))
                .map(Pastilha::getId).findFirst().orElse(null);
        Long dcmt = pastilhaRepository.findAll().stream().filter(p -> "DCMT11T304".equals(p.getCodigo()))
                .map(Pastilha::getId).findFirst().orElse(null);

        ModeloProjeto cerca = new ModeloProjeto();
        cerca.setNome("Cerca de metal (padrão)");
        cerca.setDescricao("Cerca metálica com portão, conforme medidas do cliente");
        cerca.getChecklist().addAll(List.of("Cortar os tubos nas medidas", "Soldar a estrutura da cerca",
                "Montar e soldar o portão", "Lixar e remover rebarbas", "Aplicar fundo e pintura",
                "Conferência final e embalagem"));
        if (cnmg != null) {
            cerca.getMateriais().add(new ModeloProjeto.Material(cnmg, 10));
        }
        modeloProjetoRepository.save(cerca);

        ModeloProjeto suporte = new ModeloProjeto();
        suporte.setNome("Suporte metálico (padrão)");
        suporte.setDescricao("Suporte usinado em aço para fixação de equipamentos");
        suporte.getChecklist().addAll(List.of("Preparar o material", "Usinar o suporte (torneamento)",
                "Acabamento das superfícies", "Inspeção dimensional"));
        if (cnmg != null) {
            suporte.getMateriais().add(new ModeloProjeto.Material(cnmg, 4));
        }
        if (dcmt != null) {
            suporte.getMateriais().add(new ModeloProjeto.Material(dcmt, 2));
        }
        modeloProjetoRepository.save(suporte);
    }

    private void seedFinanceiro() {
        if (fornecedorProdutoRepository.count() > 0) {
            return;
        }

        List<Fornecedor> fornecedores = fornecedorRepository.findAll();
        Fornecedor sandvik = fornecedores.get(0);
        Fornecedor mitsubishi = fornecedores.get(1);
        Fornecedor distribuidora = fornecedores.get(2);

        List<Pastilha> pastilhas = pastilhaRepository.findAll();
        Pastilha cnmg = pastilhas.get(0);
        Pastilha apmt = pastilhas.get(1);
        Pastilha dcmt = pastilhas.get(2);

        // Quem fornece cada peça e por quanto (a distribuidora vende tudo, porém mais caro).
        vincular(sandvik, cnmg, "12.50");
        vincular(sandvik, dcmt, "9.80");
        vincular(mitsubishi, apmt, "14.20");
        vincular(distribuidora, cnmg, "13.90");
        vincular(distribuidora, apmt, "15.00");
        vincular(distribuidora, dcmt, "10.50");

        for (Projeto projeto : projetoRepository.findAll()) {
            String nome = projeto.getNome();
            if (nome.startsWith("Cerca de metal")) {
                criarVenda(projeto, "Cliente ABC", "Cerca de metal sob medida, com instalação.", "4500.00", StatusVenda.PLANO, null);
            } else if (nome.startsWith("Suporte industrial")) {
                criarVenda(projeto, "Cliente XYZ", "Suporte para fixação de equipamento.", "1800.00", StatusVenda.PLANO, null);
            } else if (nome.startsWith("Base para esteira")) {
                criarVenda(projeto, "Cliente DEF", "Base de sustentação para esteira.", "5800.00", StatusVenda.VENDA,
                        LocalDate.now().minusDays(10));
            } else if (nome.startsWith("Grade de proteção")) {
                criarVenda(projeto, "Cliente GHI", "Grade de proteção para máquina.", "3200.00", StatusVenda.VENDA,
                        LocalDate.now().minusDays(20));
            }
        }
    }

    private void vincular(Fornecedor fornecedor, Pastilha pastilha, String preco) {
        FornecedorProduto fp = new FornecedorProduto();
        fp.setFornecedor(fornecedor);
        fp.setPastilha(pastilha);
        fp.setPreco(new BigDecimal(preco));
        fornecedorProdutoRepository.save(fp);
    }

    private void criarVenda(Projeto projeto, String cliente, String descricao, String valor, StatusVenda status,
            LocalDate dataVenda) {
        Venda venda = new Venda();
        venda.setProjeto(projeto);
        venda.setCliente(cliente);
        venda.setDescricao(descricao);
        venda.setValor(new BigDecimal(valor));
        venda.setStatus(status);
        venda.setDataCriacao(dataVenda != null ? dataVenda : LocalDate.now().minusDays(2));
        venda.setDataVenda(dataVenda);
        vendaRepository.save(venda);
    }

    private void adicionarMaterial(Projeto projeto, Pastilha pastilha, int quantidade) {
        ProjetoMaterial material = new ProjetoMaterial();
        material.setProjeto(projeto);
        material.setPastilha(pastilha);
        material.setQuantidade(quantidade);
        projeto.getMateriais().add(material);
    }

    private void adicionarChecklist(Projeto projeto, String[] itens, int concluidosIniciais) {
        for (int i = 0; i < itens.length; i++) {
            ChecklistItem item = new ChecklistItem();
            item.setProjeto(projeto);
            item.setTexto(itens[i]);
            item.setOrdem(i);
            item.setConcluido(i < concluidosIniciais);
            projeto.getChecklist().add(item);
        }
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
