package com.smartstock.backend.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartstock.backend.dto.UsuarioRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario criar(UsuarioRequest request, boolean atorEhAdmin) {
        if (request.getRole() == Role.ADMIN && !atorEhAdmin) {
            throw new BusinessException("Apenas administradores podem criar outra conta de administrador.");
        }
        if (request.getSenha() == null || request.getSenha().isBlank()) {
            throw new BusinessException("Informe uma senha para o novo usuário.");
        }
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail/usuário " + request.getUsername());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setUsername(request.getUsername());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(request.getRole());
        usuario.setPermissoes(request.getPermissoes());
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, UsuarioRequest request, boolean atorEhAdmin) {
        Usuario usuario = buscarPorId(id);

        if ((usuario.getRole() == Role.ADMIN || request.getRole() == Role.ADMIN) && !atorEhAdmin) {
            throw new BusinessException("Apenas administradores podem alterar uma conta de administrador.");
        }

        if (!usuario.getUsername().equals(request.getUsername())
                && usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail/usuário " + request.getUsername());
        }

        usuario.setNome(request.getNome());
        usuario.setUsername(request.getUsername());
        usuario.setRole(request.getRole());
        usuario.setPermissoes(request.getPermissoes());

        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }

        return usuarioRepository.save(usuario);
    }

    public void excluir(Long id, String usernameAtor, boolean atorEhAdmin) {
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

        usuarioRepository.delete(usuario);
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
