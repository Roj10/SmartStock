package com.smartstock.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smartstock.backend.model.Fornecedor;
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
        usuarioRepository.save(admin);

        Usuario operador = new Usuario();
        operador.setNome("Operador de Estoque");
        operador.setUsername("operador");
        operador.setSenha(passwordEncoder.encode("operador123"));
        operador.setRole(Role.OPERADOR);
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
        pastilhaRepository.save(p1);

        Pastilha p2 = new Pastilha();
        p2.setCodigo("APMT160408");
        p2.setDescricao("Pastilha de fresamento APMT 160408");
        p2.setFabricante(mitsubishi);
        p2.setEstoqueMinimo(15);
        p2.setQuantidadeAtual(8);
        pastilhaRepository.save(p2);

        Pastilha p3 = new Pastilha();
        p3.setCodigo("DCMT11T304");
        p3.setDescricao("Pastilha de acabamento DCMT 11T304");
        p3.setFabricante(sandvik);
        p3.setEstoqueMinimo(10);
        p3.setQuantidadeAtual(10);
        pastilhaRepository.save(p3);
    }
}
