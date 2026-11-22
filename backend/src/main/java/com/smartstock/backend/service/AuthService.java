package com.smartstock.backend.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.smartstock.backend.security.Segredos;
import com.smartstock.backend.security.TentativasService;

import io.jsonwebtoken.Claims;

/**
 * <b>Em palavras simples:</b> o login em etapas, com verificação em duas etapas (2FA) por e-mail.
 * <ol>
 *   <li>A pessoa digita usuário e senha.</li>
 *   <li>O sistema manda um código de 6 dígitos para o e-mail da conta e pede esse código. Assim, quem descobrir
 *       a senha ainda não entra: precisaria também do acesso ao e-mail. Na primeira vez (conta sem e-mail),
 *       a pessoa informa o e-mail antes, e o código confirma que o e-mail é mesmo dela.</li>
 *   <li>Se a senha foi definida por outra pessoa (ou é de exemplo), ela cria uma senha nova.</li>
 *   <li>Só então recebe o "crachá" (token de acesso).</li>
 * </ol>
 * Proteções: o código vale 10 minutos e uma única vez; o banco guarda só a "impressão digital" dele;
 * 5 erros cancelam o código e bloqueiam novas tentativas por 15 minutos; um código novo só pode ser pedido
 * a cada 60 segundos (evita encher a caixa de e-mail de alguém).
 */
@Service
public class AuthService {

    /** Por quantos minutos o código enviado por e-mail vale. */
    public static final int VALIDADE_CODIGO_MIN = 10;
    /** Tempo mínimo entre dois envios de código para a mesma conta. */
    public static final int ESPERA_REENVIO_S = 60;
    /** Erros com o mesmo código até ele ser cancelado. */
    private static final int ERROS_POR_CODIGO = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Segredos segredos;
    private final TentativasService tentativas;
    private final AuditoriaService auditoria;
    private final EmailService emailService;
    /** Hash usado quando a conta não existe, para a resposta demorar o mesmo tempo (não revela quais contas existem). */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            Segredos segredos, TentativasService tentativas, AuditoriaService auditoria, EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.segredos = segredos;
        this.tentativas = tentativas;
        this.auditoria = auditoria;
        this.emailService = emailService;
        this.hashFicticio = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    /** Etapa 1: usuário e senha. Se a conta já tem e-mail, o código é enviado na hora. */
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
        auditoria.registrar("LOGIN_SENHA_OK", username, "Senha correta; aguardando o código do e-mail");

        String desafio = jwtService.desafio(usuario, JwtService.CODIGO_EMAIL);
        if (!usuario.isDoisFatoresAtivo()) {
            // primeira vez (ou 2FA redefinido): a pessoa informa o e-mail que vai receber os códigos
            return RespostaLogin.proxima(Etapa.CADASTRAR_EMAIL, desafio, usuario.getNome(), null, emailService.modoTeste());
        }
        // se um código acabou de ser enviado (menos de 60 s) e ainda vale, não manda outro: usa o mesmo
        if (!codigoRecente(usuario)) {
            enviarCodigo(usuario, usuario.getEmail());
        }
        return RespostaLogin.proxima(Etapa.CODIGO_EMAIL, desafio, usuario.getNome(),
                EmailService.mascarar(usuario.getEmail()), emailService.modoTeste());
    }

    /** Etapa 2 (primeira vez): guarda o e-mail informado (ainda não confirmado) e manda o código para ele. */
    @Transactional
    public RespostaLogin cadastrarEmail(String desafio, String email) {
        Usuario usuario = validarDesafio(desafio, JwtService.CODIGO_EMAIL);
        if (usuario.isDoisFatoresAtivo()) {
            // quem já tem e-mail confirmado não troca por aqui (só o administrador, na aba Usuários)
            throw new NaoAutenticadoException("A etapa de login expirou. Entre de novo com usuário e senha.");
        }
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        if (!EmailService.emailValido(limpo)) {
            throw new BusinessException("Digite um e-mail válido, por exemplo: nome@gmail.com");
        }
        exigirEspera(usuario);
        usuario.setEmailPendente(limpo);
        enviarCodigo(usuario, limpo);
        return RespostaLogin.proxima(Etapa.CODIGO_EMAIL, jwtService.desafio(usuario, JwtService.CODIGO_EMAIL),
                usuario.getNome(), EmailService.mascarar(limpo), emailService.modoTeste());
    }

    /** Etapa 2: confere o código de 6 dígitos que chegou por e-mail. */
    @Transactional(noRollbackFor = BusinessException.class)
    public RespostaLogin verificarCodigo(String desafio, String codigo) {
        Usuario usuario = validarDesafio(desafio, JwtService.CODIGO_EMAIL);
        String chave = "2fa:" + usuario.getUsername().toLowerCase(Locale.ROOT);
        tentativas.verificar(chave);

        if (destino(usuario) == null) {
            throw new BusinessException("Informe primeiro o e-mail que vai receber o código.");
        }
        if (usuario.getCodigoEmailHash() == null || usuario.getCodigoEmailExpira() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoEmailExpira())) {
            throw new BusinessException("Este código não vale mais (venceu ou já foi usado). Clique em \"Enviar outro código\".");
        }
        String limpo = codigo.replaceAll("\\D", "");
        boolean ok = limpo.length() == 6
                && Segredos.iguais(segredos.impressaoDigital(usuario.getUsername(), limpo), usuario.getCodigoEmailHash());
        if (!ok) {
            usuario.setCodigoEmailTentativas(usuario.getCodigoEmailTentativas() + 1);
            boolean cancelado = usuario.getCodigoEmailTentativas() >= ERROS_POR_CODIGO;
            if (cancelado) {
                usuario.cancelarCodigoEmail();
            }
            usuarioRepository.save(usuario);
            boolean bloqueou = tentativas.falhou(chave);
            auditoria.registrar(bloqueou ? "2FA_BLOQUEADO" : "2FA_FALHA", usuario.getUsername(), "Código do e-mail incorreto");
            throw new BusinessException(cancelado
                    ? "Código incorreto. Depois de " + ERROS_POR_CODIGO + " erros este código foi cancelado; peça outro."
                    : "Código incorreto. Confira o último e-mail recebido.");
        }
        tentativas.acertou(chave);
        usuario.cancelarCodigoEmail(); // cada código vale uma única vez
        if (!usuario.isDoisFatoresAtivo()) {
            // o código chegou ao e-mail informado: agora ele está confirmado e passa a receber os próximos códigos
            usuario.setEmail(usuario.getEmailPendente());
            usuario.setEmailPendente(null);
            auditoria.registrar("2FA_ATIVADO", usuario.getUsername(), "E-mail confirmado: " + EmailService.mascarar(usuario.getEmail()));
        }
        usuarioRepository.save(usuario);
        return depoisDoSegundoFator(usuario);
    }

    /** Envia um código novo para o e-mail da etapa atual (no máximo um a cada 60 segundos). */
    @Transactional
    public RespostaLogin reenviarCodigo(String desafio) {
        Usuario usuario = validarDesafio(desafio, JwtService.CODIGO_EMAIL);
        String destino = destino(usuario);
        if (destino == null) {
            throw new BusinessException("Informe primeiro o e-mail que vai receber o código.");
        }
        exigirEspera(usuario);
        enviarCodigo(usuario, destino);
        return RespostaLogin.proxima(Etapa.CODIGO_EMAIL, jwtService.desafio(usuario, JwtService.CODIGO_EMAIL),
                usuario.getNome(), EmailService.mascarar(destino), emailService.modoTeste());
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
                    usuario.getNome(), null, false);
        }
        return concluido(usuario);
    }

    private RespostaLogin concluido(Usuario usuario) {
        auditoria.registrar("LOGIN_OK", usuario.getUsername(), "Login concluído com 2FA por e-mail");
        var permissoes = usuario.getRole() == Role.ADMIN
                ? Arrays.stream(Modulo.values()).map(Enum::name).collect(Collectors.toSet())
                : usuario.getPermissoes().stream().map(Enum::name).collect(Collectors.toSet());
        return new RespostaLogin(Etapa.CONCLUIDO, null, jwtService.tokenDeAcesso(usuario), usuario.getUsername(),
                usuario.getNome(), usuario.getRole().name(), permissoes, null, false);
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
        // a troca de senha só vem depois do código do e-mail confirmado
        boolean etapaCoerente = !JwtService.TROCAR_SENHA.equals(tipoEsperado) || usuario.isDoisFatoresAtivo();
        if (versao == null || versao != usuario.getTokenVersao() || !etapaCoerente) {
            throw new NaoAutenticadoException("A etapa de login expirou. Entre de novo com usuário e senha.");
        }
        return usuario;
    }

    /** Para onde vai o código: o e-mail confirmado ou, na primeira vez, o que a pessoa acabou de informar. */
    private static String destino(Usuario usuario) {
        return usuario.isDoisFatoresAtivo() ? usuario.getEmail() : usuario.getEmailPendente();
    }

    /** Gera um código aleatório de 6 dígitos, guarda só a impressão digital dele e envia por e-mail. */
    private void enviarCodigo(Usuario usuario, String destino) {
        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        usuario.novoCodigoEmail(segredos.impressaoDigital(usuario.getUsername(), codigo), LocalDateTime.now(),
                VALIDADE_CODIGO_MIN);
        usuarioRepository.save(usuario);
        emailService.enviarCodigo(destino, usuario.getNome(), codigo, VALIDADE_CODIGO_MIN);
        auditoria.registrar("2FA_CODIGO_ENVIADO", usuario.getUsername(), "Código enviado para " + EmailService.mascarar(destino));
    }

    /** Um código foi enviado há menos de 60 s e ainda vale? */
    private static boolean codigoRecente(Usuario usuario) {
        return usuario.getCodigoEmailHash() != null && usuario.getCodigoEmailEnviado() != null
                && usuario.getCodigoEmailEnviado().plusSeconds(ESPERA_REENVIO_S).isAfter(LocalDateTime.now());
    }

    /** Recusa pedir outro código antes de 60 s do último envio. */
    private static void exigirEspera(Usuario usuario) {
        if (usuario.getCodigoEmailEnviado() == null) {
            return;
        }
        long faltam = ESPERA_REENVIO_S - Duration.between(usuario.getCodigoEmailEnviado(), LocalDateTime.now()).getSeconds();
        if (faltam > 0) {
            throw new BusinessException("Aguarde " + faltam + " segundos para pedir outro código.");
        }
    }
}
