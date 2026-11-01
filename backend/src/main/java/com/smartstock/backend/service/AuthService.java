package com.smartstock.backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.backend.dto.AuthDtos.ConfiguracaoDoisFatores;
import com.smartstock.backend.dto.AuthDtos.Etapa;
import com.smartstock.backend.dto.AuthDtos.RespostaLogin;
import com.smartstock.backend.dto.LoginRequest;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.NaoAutenticadoException;
import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.UsuarioRepository;
import com.smartstock.backend.security.JwtService;
import com.smartstock.backend.security.QrCode;
import com.smartstock.backend.security.Segredos;
import com.smartstock.backend.security.TentativasService;
import com.smartstock.backend.security.Totp;

import io.jsonwebtoken.Claims;

/**
 * Login em etapas:
 * 1. usuário e senha;
 * 2. 2FA obrigatório: na primeira vez, configurar o app autenticador (QR code) e confirmar um código, recebendo os
 *    códigos de recuperação; nas próximas, digitar o código do app (ou um código de recuperação);
 * 3. troca de senha, quando a conta usa senha de exemplo ou definida pelo administrador;
 * 4. só então o token de acesso é entregue.
 */
@Service
public class AuthService {

    private static final String EMISSOR = "SmartStock";
    private static final String ALFABETO_RECUPERACAO = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Segredos segredos;
    private final TentativasService tentativas;
    private final AuditoriaService auditoria;
    /** Hash usado quando a conta não existe, para a resposta demorar o mesmo tempo (não revela quais contas existem). */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            Segredos segredos, TentativasService tentativas, AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.segredos = segredos;
        this.tentativas = tentativas;
        this.auditoria = auditoria;
        this.hashFicticio = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    /** Etapa 1: usuário e senha. */
    @Transactional
    public RespostaLogin login(LoginRequest request) {
        String username = request.getUsername().trim();
        String chave = "login:" + username.toLowerCase(Locale.ROOT);
        tentativas.verificar(chave);

        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        boolean confere = passwordEncoder.matches(request.getSenha(), usuario != null ? usuario.getSenha() : hashFicticio);
        if (usuario == null || !confere) {
            boolean bloqueou = tentativas.falhou(chave);
            auditoria.registrar(bloqueou ? "LOGIN_BLOQUEADO" : "LOGIN_FALHA", username,
                    bloqueou ? "Bloqueado por 15 min após " + TentativasService.LIMITE + " senhas erradas" : "Senha errada");
            throw new NaoAutenticadoException("Usuário ou senha inválidos.");
        }
        tentativas.acertou(chave);
        auditoria.registrar("LOGIN_SENHA_OK", username, "Senha correta; aguardando o 2FA");

        Etapa etapa = usuario.isDoisFatoresAtivo() ? Etapa.CODIGO_2FA : Etapa.CONFIGURAR_2FA;
        String tipo = usuario.isDoisFatoresAtivo() ? JwtService.CODIGO_2FA : JwtService.CONFIGURAR_2FA;
        return RespostaLogin.proxima(etapa, jwtService.desafio(usuario, tipo), usuario.getNome(), null);
    }

    /** Etapa 2a (primeira vez): gera o segredo e o QR code para o app autenticador. */
    @Transactional
    public ConfiguracaoDoisFatores configurarDoisFatores(String desafio) {
        Usuario usuario = validarDesafio(desafio, JwtService.CONFIGURAR_2FA);
        String segredo = Totp.novoSegredo();
        usuario.setTotpPendente(segredos.cifrar(segredo));
        usuarioRepository.save(usuario);
        String uri = Totp.uri(EMISSOR, usuario.getUsername(), segredo);
        return new ConfiguracaoDoisFatores(QrCode.pngDataUrl(uri, 6), agrupar(segredo), usuario.getUsername());
    }

    /** Etapa 2a: confirma o primeiro código do app, ativa o 2FA e entrega os códigos de recuperação. */
    @Transactional
    public RespostaLogin ativarDoisFatores(String desafio, String codigo) {
        Usuario usuario = validarDesafio(desafio, JwtService.CONFIGURAR_2FA);
        if (usuario.getTotpPendente() == null) {
            throw new BusinessException("Gere o QR code antes de confirmar o código.");
        }
        String chave = "2fa:" + usuario.getUsername().toLowerCase(Locale.ROOT);
        tentativas.verificar(chave);
        String segredo = segredos.decifrar(usuario.getTotpPendente());
        OptionalLong passo = Totp.verificar(segredo, codigo, Instant.now().getEpochSecond());
        if (passo.isEmpty()) {
            tentativas.falhou(chave);
            throw new BusinessException("Código incorreto. Confira se o relógio do celular está certo e use o código atual.");
        }
        tentativas.acertou(chave);
        usuario.setTotpSegredo(usuario.getTotpPendente());
        usuario.setTotpPendente(null);
        usuario.setTotpUltimoPasso(passo.getAsLong());
        usuario.setDoisFatoresAtivo(true);
        List<String> codigos = novosCodigosRecuperacao(usuario);
        usuarioRepository.save(usuario);
        auditoria.registrar("2FA_ATIVADO", usuario.getUsername(), "App autenticador configurado");
        RespostaLogin seguinte = depoisDoSegundoFator(usuario);
        return new RespostaLogin(seguinte.etapa(), seguinte.desafio(), seguinte.token(), seguinte.username(),
                seguinte.nome(), seguinte.role(), seguinte.permissoes(), codigos);
    }

    /** Etapa 2b: código do app (6 dígitos) ou um código de recuperação (uso único). */
    @Transactional
    public RespostaLogin verificarDoisFatores(String desafio, String codigo) {
        Usuario usuario = validarDesafio(desafio, JwtService.CODIGO_2FA);
        String chave = "2fa:" + usuario.getUsername().toLowerCase(Locale.ROOT);
        tentativas.verificar(chave);

        String limpo = codigo.trim().toLowerCase(Locale.ROOT);
        boolean ok;
        if (limpo.replaceAll("\\s", "").matches("\\d{6}")) {
            OptionalLong passo = Totp.verificar(segredos.decifrar(usuario.getTotpSegredo()), limpo,
                    Instant.now().getEpochSecond());
            // o mesmo código (ou um mais antigo) não vale duas vezes
            ok = passo.isPresent() && passo.getAsLong() > usuario.getTotpUltimoPasso();
            if (ok) {
                usuario.setTotpUltimoPasso(passo.getAsLong());
            }
        } else {
            ok = usarCodigoRecuperacao(usuario, limpo);
        }
        if (!ok) {
            boolean bloqueou = tentativas.falhou(chave);
            auditoria.registrar(bloqueou ? "2FA_BLOQUEADO" : "2FA_FALHA", usuario.getUsername(), "Código do 2FA incorreto");
            throw new BusinessException("Código incorreto ou já usado.");
        }
        tentativas.acertou(chave);
        usuarioRepository.save(usuario);
        return depoisDoSegundoFator(usuario);
    }

    /** Etapa 3: troca de senha obrigatória. */
    @Transactional
    public RespostaLogin trocarSenha(String desafio, String novaSenha) {
        Usuario usuario = validarDesafio(desafio, JwtService.TROCAR_SENHA);
        PoliticaSenha.validar(novaSenha, usuario.getUsername());
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new BusinessException("A senha nova precisa ser diferente da atual.");
        }
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setTrocarSenha(false);
        usuario.encerrarSessoes();
        usuarioRepository.save(usuario);
        auditoria.registrar("SENHA_TROCADA", usuario.getUsername(), "Troca obrigatória no login");
        return concluido(usuario);
    }

    /** Sair: derruba todas as sessões abertas da conta (em todas as abas e computadores). */
    @Transactional
    public void sair(String username) {
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            u.encerrarSessoes();
            usuarioRepository.save(u);
            auditoria.registrar("SAIDA", username, "Sessões encerradas");
        });
    }

    private RespostaLogin depoisDoSegundoFator(Usuario usuario) {
        if (usuario.isTrocarSenha()) {
            return RespostaLogin.proxima(Etapa.TROCAR_SENHA, jwtService.desafio(usuario, JwtService.TROCAR_SENHA),
                    usuario.getNome(), null);
        }
        return concluido(usuario);
    }

    private RespostaLogin concluido(Usuario usuario) {
        auditoria.registrar("LOGIN_OK", usuario.getUsername(), "Login concluído com 2FA");
        var permissoes = usuario.getRole() == Role.ADMIN
                ? Arrays.stream(Modulo.values()).map(Enum::name).collect(Collectors.toSet())
                : usuario.getPermissoes().stream().map(Enum::name).collect(Collectors.toSet());
        return new RespostaLogin(Etapa.CONCLUIDO, null, jwtService.tokenDeAcesso(usuario), usuario.getUsername(),
                usuario.getNome(), usuario.getRole().name(), permissoes, null);
    }

    /** Confere o token curto da etapa: assinatura, validade, tipo e versão da conta. */
    private Usuario validarDesafio(String desafio, String tipoEsperado) {
        Claims claims = jwtService.ler(desafio);
        if (claims == null || !tipoEsperado.equals(claims.get("tipo", String.class))) {
            throw new NaoAutenticadoException("A etapa de login expirou. Entre de novo com usuário e senha.");
        }
        Usuario usuario = usuarioRepository.findByUsername(claims.getSubject())
                .orElseThrow(() -> new NaoAutenticadoException("A etapa de login expirou. Entre de novo com usuário e senha."));
        Integer versao = claims.get("ver", Integer.class);
        boolean etapaCoerente = switch (tipoEsperado) {
            case JwtService.CONFIGURAR_2FA -> !usuario.isDoisFatoresAtivo();
            case JwtService.CODIGO_2FA, JwtService.TROCAR_SENHA -> usuario.isDoisFatoresAtivo();
            default -> false;
        };
        if (versao == null || versao != usuario.getTokenVersao() || !etapaCoerente) {
            throw new NaoAutenticadoException("A etapa de login expirou. Entre de novo com usuário e senha.");
        }
        return usuario;
    }

    private List<String> novosCodigosRecuperacao(Usuario usuario) {
        List<String> codigos = new ArrayList<>();
        usuario.getCodigosRecuperacao().clear();
        for (int i = 0; i < 8; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                if (j == 4) {
                    sb.append('-');
                }
                sb.append(ALFABETO_RECUPERACAO.charAt(ALEATORIO.nextInt(ALFABETO_RECUPERACAO.length())));
            }
            codigos.add(sb.toString());
            usuario.getCodigosRecuperacao().add(passwordEncoder.encode(sb.toString()));
        }
        return codigos;
    }

    private boolean usarCodigoRecuperacao(Usuario usuario, String codigo) {
        String normalizado = codigo.replaceAll("[\\s-]", "");
        if (normalizado.length() != 8) {
            return false;
        }
        String formatado = normalizado.substring(0, 4) + "-" + normalizado.substring(4);
        for (String hash : List.copyOf(usuario.getCodigosRecuperacao())) {
            if (passwordEncoder.matches(formatado, hash)) {
                usuario.getCodigosRecuperacao().remove(hash);
                auditoria.registrar("2FA_RECUPERACAO", usuario.getUsername(),
                        "Código de recuperação usado; restam " + usuario.getCodigosRecuperacao().size());
                return true;
            }
        }
        return false;
    }

    private static String agrupar(String segredo) {
        return segredo.replaceAll("(.{4})(?!$)", "$1 ");
    }
}
