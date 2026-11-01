package com.smartstock.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.UsuarioRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;

/**
 * <b>Em palavras simples:</b> Regras das contas: criar, editar, excluir, escolher abas e redefinir o 2FA.
 *
 * Proteções: ninguém exclui a própria conta nem o último administrador; quem não é administrador só
 * pode dar abas que ele mesmo tem; ao trocar a senha de alguém, as sessões dessa pessoa são encerradas e
 * ela precisa escolher uma senha nova no próximo login.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MensagemService mensagemService;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            MensagemService mensagemService, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.mensagemService = mensagemService;
        this.auditoria = auditoria;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public Usuario criar(UsuarioRequest request, String usernameAtor) {
        Usuario ator = buscarPorUsername(usernameAtor);
        boolean atorEhAdmin = ator.getRole() == Role.ADMIN;
        if (request.getRole() == Role.ADMIN && !atorEhAdmin) {
            throw new BusinessException("Apenas administradores podem criar outra conta de administrador.");
        }
        if (request.getSenha() == null || request.getSenha().isBlank()) {
            throw new BusinessException("Informe uma senha para o novo usuário.");
        }
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail/usuário " + request.getUsername());
        }
        conferirPermissoesConcedidas(ator, request.getPermissoes());
        PoliticaSenha.validar(request.getSenha(), request.getUsername());

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setUsername(request.getUsername());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(request.getRole());
        usuario.setPermissoes(request.getPermissoes());
        // a senha foi escolhida por outra pessoa: a dona da conta troca no primeiro login (e configura o 2FA)
        usuario.setTrocarSenha(true);
        Usuario salvo = usuarioRepository.save(usuario);
        auditoria.registrar("USUARIO_CRIADO", usernameAtor, "Criou a conta " + salvo.getUsername() + " ("
                + salvo.getRole() + ", abas: " + salvo.getPermissoes() + ")");
        return salvo;
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioRequest request, String usernameAtor) {
        Usuario ator = buscarPorUsername(usernameAtor);
        boolean atorEhAdmin = ator.getRole() == Role.ADMIN;
        Usuario usuario = buscarPorId(id);
        boolean propria = usuario.getId().equals(ator.getId());

        if ((usuario.getRole() == Role.ADMIN || request.getRole() == Role.ADMIN) && !atorEhAdmin) {
            throw new BusinessException("Apenas administradores podem alterar uma conta de administrador.");
        }
        Set<Modulo> novas = request.getPermissoes() != null ? request.getPermissoes() : Set.of();
        if (!atorEhAdmin) {
            // quem só tem a aba Usuários não pode aumentar o próprio acesso nem dar a outros o que não tem
            if (propria && (!novas.equals(usuario.getPermissoes()) || request.getRole() != usuario.getRole())) {
                throw new BusinessException("Você não pode alterar as permissões da própria conta.");
            }
            Set<Modulo> acrescentadas = new HashSet<>(novas);
            acrescentadas.removeAll(usuario.getPermissoes());
            conferirPermissoesConcedidas(ator, acrescentadas);
        }
        if (!usuario.getUsername().equals(request.getUsername())
                && usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail/usuário " + request.getUsername());
        }

        boolean mudouAcesso = !novas.equals(usuario.getPermissoes()) || request.getRole() != usuario.getRole();
        usuario.setNome(request.getNome());
        usuario.setUsername(request.getUsername());
        usuario.setRole(request.getRole());
        usuario.setPermissoes(request.getPermissoes());

        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            PoliticaSenha.validar(request.getSenha(), request.getUsername());
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
            // senha definida por outra pessoa: troca obrigatória no próximo login; derruba as sessões abertas
            usuario.setTrocarSenha(!propria);
            usuario.encerrarSessoes();
            auditoria.registrar("SENHA_REDEFINIDA", usernameAtor, "Redefiniu a senha de " + usuario.getUsername());
        }
        if (mudouAcesso) {
            auditoria.registrar("PERMISSOES_ALTERADAS", usernameAtor, "Conta " + usuario.getUsername() + ": "
                    + usuario.getRole() + ", abas " + usuario.getPermissoes());
        }
        return usuarioRepository.save(usuario);
    }

    /** O administrador desliga o 2FA de uma conta (celular perdido): ela configura de novo no próximo login. */
    @Transactional
    public Usuario redefinirDoisFatores(Long id, String usernameAtor) {
        Usuario usuario = buscarPorId(id);
        usuario.limparDoisFatores();
        usuario.encerrarSessoes();
        auditoria.registrar("2FA_REDEFINIDO", usernameAtor, "Redefiniu o 2FA de " + usuario.getUsername());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluir(Long id, String usernameAtor) {
        Usuario ator = buscarPorUsername(usernameAtor);
        boolean atorEhAdmin = ator.getRole() == Role.ADMIN;
        Usuario usuario = buscarPorId(id);

        if (usuario.getUsername().equals(usernameAtor)) {
            throw new BusinessException("Você não pode excluir a própria conta.");
        }
        if (usuario.getRole() == Role.ADMIN) {
            if (!atorEhAdmin) {
                throw new BusinessException("Apenas administradores podem excluir uma conta de administrador.");
            }
            if (usuarioRepository.countByRole(Role.ADMIN) <= 1) {
                throw new BusinessException("Não é possível excluir o único administrador do sistema.");
            }
        }

        // as conversas da conta excluída saem junto
        mensagemService.excluirConversasDoUsuario(id);
        usuarioRepository.delete(usuario);
        auditoria.registrar("USUARIO_EXCLUIDO", usernameAtor, "Excluiu a conta " + usuario.getUsername());
    }

    /** Quem não é administrador só pode conceder abas que ele mesmo tem. */
    private static void conferirPermissoesConcedidas(Usuario ator, Set<Modulo> concedidas) {
        if (ator.getRole() == Role.ADMIN || concedidas == null) {
            return;
        }
        Set<Modulo> semDireito = new HashSet<>(concedidas);
        semDireito.removeAll(ator.getPermissoes());
        if (!semDireito.isEmpty()) {
            throw new BusinessException("Você não pode conceder acesso a abas que a sua conta não tem: " + semDireito);
        }
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
