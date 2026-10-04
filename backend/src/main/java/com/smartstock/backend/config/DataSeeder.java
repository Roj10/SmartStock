package com.smartstock.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
        mensagem(operador, financeiro, "Bom dia! O estoque de tinta esmalte está ficando baixo para a próxima ordem de produção. "
                + "Dá para comprar mais 20 latas?", agora.minusMinutes(40), true);
        mensagem(financeiro, operador, "Bom dia! Vou consultar os fornecedores e te aviso o prazo de entrega.",
                agora.minusMinutes(25), true);
        mensagem(financeiro, operador,
                "Consegui com a Tintas Serrana: chega na quinta. Pode dar a entrada no sistema quando receber.",
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

        criarFornecedor("Siderúrgica Planalto", TipoFornecedor.FABRICANTE, "Vendas de aço", "(47) 3333-1111",
                "vendas@planalto.example.com");
        criarFornecedor("Tintas Serrana", TipoFornecedor.FABRICANTE, "Atendimento ao cliente", "(47) 3333-2222",
                "atendimento@serrana.example.com");
        criarFornecedor("Ferragens Fraiburgo", TipoFornecedor.FORNECEDOR, "Vendas", "(49) 3222-4444",
                "vendas@ferragensfraiburgo.example.com");
    }

    private void criarFornecedor(String nome, TipoFornecedor tipo, String contato, String telefone, String email) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNome(nome);
        fornecedor.setTipo(tipo);
        fornecedor.setContato(contato);
        fornecedor.setTelefone(telefone);
        fornecedor.setEmail(email);
        fornecedorRepository.save(fornecedor);
    }

    private Fornecedor fornecedor(String nome) {
        return fornecedorRepository.findAll().stream().filter(f -> f.getNome().equals(nome)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Fornecedor de exemplo não encontrado: " + nome));
    }

    private Pastilha produto(String codigo) {
        return pastilhaRepository.findAll().stream().filter(p -> p.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Produto de exemplo não encontrado: " + codigo));
    }

    /** Materiais de uma metalúrgica, com fotos reais (arquivos em src/main/resources/seed-images). */
    private void seedPastilhas() {
        if (pastilhaRepository.count() > 0) {
            return;
        }

        Fornecedor planalto = fornecedor("Siderúrgica Planalto");
        Fornecedor serrana = fornecedor("Tintas Serrana");
        Fornecedor ferragens = fornecedor("Ferragens Fraiburgo");

        criarProduto("BAR-FE-01", "Barra de ferro quadrada 1\" (barra de 6 m)", planalto, 20, 45, "barra-ferro-chata.jpg");
        criarProduto("RDA-PORT-150", "Roda de portão em ferro fundido 150 mm", ferragens, 10, 14, "roda-portao.jpg");
        criarProduto("TIN-ESM-04", "Tinta esmalte sintético exterior (lata 0,4 kg)", serrana, 12, 8, "tinta-esmalte.jpg");
        criarProduto("VER-DAM-75", "Verniz damar incolor (frasco 75 ml)", serrana, 8, 20, "verniz.jpg");
        criarProduto("ELE-6013-25", "Eletrodo de solda E6013 2,5 mm (caixa 5 kg)", planalto, 15, 28, "eletrodo-e6013.jpg");
        criarProduto("DIS-COR-125", "Disco de corte para aço 125 mm (1 mm)", ferragens, 30, 52, "disco-corte.jpg");
        criarProduto("DOB-PORT-REF", "Dobradiça de portão reforçada com pino regulável", ferragens, 10, 10,
                "dobradica-portao.jpg");
        criarProduto("TUB-ACO-48", "Tubo de aço inox 304 redondo 1.1/2\" (barra de 6 m)", planalto, 15, 30, "tubo-aco.jpg");
    }

    private void criarProduto(String codigo, String descricao, Fornecedor fabricante, int minimo, int atual, String imagem) {
        Pastilha p = new Pastilha();
        p.setCodigo(codigo);
        p.setDescricao(descricao);
        p.setFabricante(fabricante);
        p.setEstoqueMinimo(minimo);
        p.setQuantidadeAtual(atual);
        p.setImagemUrl(copiarImagemExemplo(imagem));
        pastilhaRepository.save(p);
    }

    private void seedProjetos() {
        if (projetoRepository.count() > 0) {
            return;
        }

        Pastilha barra = produto("BAR-FE-01");
        Pastilha roda = produto("RDA-PORT-150");
        Pastilha tinta = produto("TIN-ESM-04");
        Pastilha eletrodo = produto("ELE-6013-25");
        Pastilha disco = produto("DIS-COR-125");
        Pastilha dobradica = produto("DOB-PORT-REF");
        Pastilha tubo = produto("TUB-ACO-48");

        // Exemplo citado pelo usuário: projeto de cerca de metal, em produção,
        // com parte do checklist já concluída.
        Projeto cercaMetal = new Projeto();
        cercaMetal.setNome("Cerca de metal - Cliente ABC");
        cercaMetal.setDescricao("Cerca de metal sob medida para o pátio do cliente.");
        cercaMetal.setStatus(StatusProjeto.EM_PRODUCAO);
        cercaMetal.setDataCriacao(LocalDateTime.now().minusDays(3));
        cercaMetal.setDataInicioProducao(LocalDateTime.now().minusDays(2));
        adicionarMaterial(cercaMetal, barra, 10);
        adicionarMaterial(cercaMetal, tubo, 4);
        adicionarMaterial(cercaMetal, eletrodo, 2);
        adicionarMaterial(cercaMetal, tinta, 2);
        adicionarChecklist(cercaMetal,
                new String[] { "Separar os materiais da lista", "Fazer o ligamento (solda) das peças",
                        "Fazer as rodas", "Pintura", "Finalização e conferência" },
                2);
        projetoRepository.save(cercaMetal);

        Projeto suporteIndustrial = new Projeto();
        suporteIndustrial.setNome("Suporte industrial - Cliente XYZ");
        suporteIndustrial.setDescricao("Suporte metálico para fixação de equipamento.");
        suporteIndustrial.setStatus(StatusProjeto.AGUARDANDO);
        suporteIndustrial.setDataCriacao(LocalDateTime.now().minusHours(6));
        adicionarMaterial(suporteIndustrial, barra, 4);
        adicionarMaterial(suporteIndustrial, disco, 6);
        adicionarMaterial(suporteIndustrial, eletrodo, 1);
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
        adicionarMaterial(portao, roda, 4);
        adicionarMaterial(portao, dobradica, 4);
        adicionarChecklist(portao, new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(portao);

        Projeto escada = new Projeto();
        escada.setNome("Escada marinheiro - Cliente MNO");
        escada.setDescricao("Escada de acesso ao reservatório, liberada para entrega.");
        escada.setStatus(StatusProjeto.PRONTO_ENTREGA);
        escada.setCliente("Cliente MNO");
        adicionarMaterial(escada, tubo, 2);
        adicionarMaterial(escada, tinta, 1);
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
        adicionarMaterial(baseEsteira, barra, 12);
        adicionarMaterial(baseEsteira, eletrodo, 3);
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
        adicionarMaterial(gradeProtecao, tubo, 5);
        adicionarMaterial(gradeProtecao, disco, 8);
        adicionarChecklist(gradeProtecao,
                new String[] { "Separar materiais", "Corte e solda", "Pintura", "Finalização" }, 4);
        projetoRepository.save(gradeProtecao);
    }

    /** Modelos (padrões) de projeto de exemplo, para criar novos pedidos do mesmo tipo rapidamente. */
    private void seedModelos() {
        if (modeloProjetoRepository.count() > 0) {
            return;
        }
        Long barra = produto("BAR-FE-01").getId();
        Long tubo = produto("TUB-ACO-48").getId();
        Long eletrodo = produto("ELE-6013-25").getId();
        Long tinta = produto("TIN-ESM-04").getId();
        Long disco = produto("DIS-COR-125").getId();

        ModeloProjeto cerca = new ModeloProjeto();
        cerca.setNome("Cerca de metal (padrão)");
        cerca.setDescricao("Cerca metálica com portão, conforme medidas do cliente");
        cerca.getChecklist().addAll(List.of("Cortar os tubos nas medidas", "Soldar a estrutura da cerca",
                "Montar e soldar o portão", "Lixar e remover rebarbas", "Aplicar fundo e pintura",
                "Conferência final e embalagem"));
        cerca.getMateriais().add(new ModeloProjeto.Material(barra, 10));
        cerca.getMateriais().add(new ModeloProjeto.Material(tubo, 4));
        cerca.getMateriais().add(new ModeloProjeto.Material(eletrodo, 2));
        cerca.getMateriais().add(new ModeloProjeto.Material(tinta, 2));
        modeloProjetoRepository.save(cerca);

        ModeloProjeto suporte = new ModeloProjeto();
        suporte.setNome("Suporte metálico (padrão)");
        suporte.setDescricao("Suporte em aço para fixação de equipamentos");
        suporte.getChecklist().addAll(List.of("Preparar o material", "Cortar e furar as peças",
                "Soldar e montar o suporte", "Acabamento das superfícies", "Inspeção dimensional"));
        suporte.getMateriais().add(new ModeloProjeto.Material(barra, 4));
        suporte.getMateriais().add(new ModeloProjeto.Material(disco, 6));
        suporte.getMateriais().add(new ModeloProjeto.Material(eletrodo, 1));
        modeloProjetoRepository.save(suporte);
    }

    private void seedFinanceiro() {
        if (fornecedorProdutoRepository.count() > 0) {
            return;
        }

        Fornecedor planalto = fornecedor("Siderúrgica Planalto");
        Fornecedor serrana = fornecedor("Tintas Serrana");
        Fornecedor ferragens = fornecedor("Ferragens Fraiburgo");

        Pastilha barra = produto("BAR-FE-01");
        Pastilha roda = produto("RDA-PORT-150");
        Pastilha tinta = produto("TIN-ESM-04");
        Pastilha verniz = produto("VER-DAM-75");
        Pastilha eletrodo = produto("ELE-6013-25");
        Pastilha disco = produto("DIS-COR-125");
        Pastilha dobradica = produto("DOB-PORT-REF");
        Pastilha tubo = produto("TUB-ACO-48");

        // Quem fornece cada item e por quanto (a loja de ferragens vende quase tudo, porém mais caro).
        vincular(planalto, barra, "62.00");
        vincular(planalto, tubo, "118.00");
        vincular(planalto, eletrodo, "24.50");
        vincular(serrana, tinta, "18.90");
        vincular(serrana, verniz, "14.50");
        vincular(ferragens, roda, "38.90");
        vincular(ferragens, dobradica, "21.50");
        vincular(ferragens, disco, "6.40");
        vincular(ferragens, barra, "68.00");
        vincular(ferragens, tubo, "129.00");
        vincular(ferragens, eletrodo, "26.90");
        vincular(ferragens, tinta, "22.00");
        vincular(ferragens, verniz, "17.90");

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
     * Copia uma foto de exemplo (guardada em src/main/resources/seed-images, com os créditos em CREDITOS.md)
     * para a pasta de uploads, de onde o sistema serve as imagens dos produtos.
     */
    private String copiarImagemExemplo(String arquivo) {
        try (InputStream origem = getClass().getResourceAsStream("/seed-images/" + arquivo)) {
            if (origem == null) {
                return null;
            }
            Path diretorio = Path.of(uploadsDir, "pastilhas");
            Files.createDirectories(diretorio);
            String nomeArquivo = "seed-" + arquivo;
            Files.copy(origem, diretorio.resolve(nomeArquivo), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/pastilhas/" + nomeArquivo;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao copiar a imagem de exemplo " + arquivo, e);
        }
    }
}
