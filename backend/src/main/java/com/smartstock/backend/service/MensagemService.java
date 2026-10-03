package com.smartstock.backend.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.backend.dto.MensagemDtos.Contato;
import com.smartstock.backend.dto.MensagemDtos.ImagemMensagem;
import com.smartstock.backend.dto.MensagemDtos.MensagemResposta;
import com.smartstock.backend.dto.MensagemDtos.NaoLidas;
import com.smartstock.backend.exception.BusinessException;
import com.smartstock.backend.exception.ResourceNotFoundException;
import com.smartstock.backend.model.Mensagem;
import com.smartstock.backend.model.Usuario;
import com.smartstock.backend.repository.MensagemRepository;
import com.smartstock.backend.repository.UsuarioRepository;

@Service
public class MensagemService {

    /** Quantas mensagens de uma conversa são devolvidas de uma vez (as mais recentes). */
    private static final int LIMITE_CONVERSA = 200;

    private static final int LIMITE_TEXTO = 2000;

    private final MensagemRepository mensagemRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.mensagens.dir}")
    private String mensagensDir;

    public MensagemService(MensagemRepository mensagemRepository, UsuarioRepository usuarioRepository) {
        this.mensagemRepository = mensagemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Todas as outras contas, com a última mensagem trocada e quantas ainda não foram lidas. */
    @Transactional(readOnly = true)
    public List<Contato> listarContatos() {
        Usuario eu = usuarioAtual();
        List<Contato> contatos = new ArrayList<>();
        for (Usuario outro : usuarioRepository.findAll()) {
            if (outro.getId().equals(eu.getId())) {
                continue;
            }
            List<Mensagem> ultima = mensagemRepository.conversa(eu.getId(), outro.getId(), PageRequest.of(0, 1));
            long naoLidas = mensagemRepository.countByDestinatarioIdAndRemetenteIdAndLidaFalse(eu.getId(),
                    outro.getId());
            contatos.add(new Contato(outro.getId(), outro.getNome(), outro.getRole(), outro.getPermissoes(),
                    ultima.isEmpty() ? null : resposta(ultima.get(0)), naoLidas));
        }
        // conversas mais recentes primeiro; quem nunca trocou mensagem fica no fim, em ordem alfabética
        contatos.sort(Comparator
                .comparing((Contato c) -> c.ultimaMensagem() == null ? null : c.ultimaMensagem().dataHora(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Contato::nome, String.CASE_INSENSITIVE_ORDER));
        return contatos;
    }

    /** Abre a conversa com uma conta: devolve as mensagens (da mais antiga para a mais nova) e marca as recebidas como lidas. */
    @Transactional
    public List<MensagemResposta> abrirConversa(Long outroId) {
        Usuario eu = usuarioAtual();
        buscarOutro(eu, outroId);
        mensagemRepository.marcarComoLidas(eu.getId(), outroId);
        List<Mensagem> recentes = new ArrayList<>(
                mensagemRepository.conversa(eu.getId(), outroId, PageRequest.of(0, LIMITE_CONVERSA)));
        Collections.reverse(recentes);
        return recentes.stream().map(this::resposta).toList();
    }

    @Transactional
    public MensagemResposta enviar(Long destinatarioId, String texto) {
        Usuario eu = usuarioAtual();
        Usuario destinatario = buscarOutro(eu, destinatarioId);

        Mensagem mensagem = nova(eu, destinatario, texto);
        return resposta(mensagemRepository.save(mensagem));
    }

    /** Envia uma imagem (com uma legenda opcional). */
    @Transactional
    public MensagemResposta enviarImagem(Long destinatarioId, String legenda, MultipartFile arquivo) {
        Usuario eu = usuarioAtual();
        Usuario destinatario = buscarOutro(eu, destinatarioId);

        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Selecione uma imagem para enviar.");
        }
        if (legenda != null && legenda.trim().length() > LIMITE_TEXTO) {
            throw new BusinessException("A mensagem pode ter no máximo " + LIMITE_TEXTO + " caracteres.");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a imagem enviada.", e);
        }
        TipoImagem tipo = TipoImagem.detectar(conteudo);
        if (tipo == null) {
            throw new BusinessException("Formato de imagem não suportado. Use PNG, JPEG, WEBP ou GIF.");
        }

        String nomeArquivo = UUID.randomUUID() + tipo.extensao;
        try {
            Path diretorio = Path.of(mensagensDir);
            Files.createDirectories(diretorio);
            Files.write(diretorio.resolve(nomeArquivo), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a imagem da mensagem.", e);
        }

        Mensagem mensagem = nova(eu, destinatario, legenda == null ? "" : legenda);
        mensagem.setImagemArquivo(nomeArquivo);
        mensagem.setImagemTipo(tipo.mime);
        return resposta(mensagemRepository.save(mensagem));
    }

    /** Devolve a imagem de uma mensagem, somente para quem enviou ou recebeu. */
    @Transactional(readOnly = true)
    public ImagemMensagem buscarImagem(Long mensagemId) {
        Usuario eu = usuarioAtual();
        Mensagem mensagem = mensagemRepository.findById(mensagemId)
                .filter(m -> m.getImagemArquivo() != null)
                .filter(m -> m.getRemetente().getId().equals(eu.getId())
                        || m.getDestinatario().getId().equals(eu.getId()))
                // mesma resposta para "não existe" e "não é sua": não revela mensagens de terceiros
                .orElseThrow(() -> new ResourceNotFoundException("Imagem não encontrada."));

        Path arquivo = Path.of(mensagensDir).resolve(mensagem.getImagemArquivo()).normalize();
        if (!arquivo.startsWith(Path.of(mensagensDir).normalize()) || !Files.isReadable(arquivo)) {
            throw new ResourceNotFoundException("Imagem não encontrada.");
        }
        Resource recurso = new FileSystemResource(arquivo);
        return new ImagemMensagem(recurso, mensagem.getImagemTipo());
    }

    @Transactional(readOnly = true)
    public NaoLidas contarNaoLidas() {
        Long meuId = usuarioAtual().getId();
        long total = mensagemRepository.countByDestinatarioIdAndLidaFalse(meuId);
        String ultimoRemetente = total == 0 ? null
                : mensagemRepository.findFirstByDestinatarioIdAndLidaFalseOrderByDataHoraDescIdDesc(meuId)
                        .map(m -> m.getRemetente().getNome()).orElse(null);
        return new NaoLidas(total, ultimoRemetente);
    }

    /** Apaga as conversas de uma conta que está sendo excluída, inclusive os arquivos de imagem. */
    @Transactional
    public void excluirConversasDoUsuario(Long usuarioId) {
        for (Mensagem m : mensagemRepository.comImagemDoUsuario(usuarioId)) {
            try {
                Files.deleteIfExists(Path.of(mensagensDir).resolve(m.getImagemArquivo()));
            } catch (IOException ignored) {
                // melhor esforço: não impede a exclusão da conta
            }
        }
        mensagemRepository.excluirDoUsuario(usuarioId);
    }

    private Mensagem nova(Usuario remetente, Usuario destinatario, String texto) {
        Mensagem mensagem = new Mensagem();
        mensagem.setRemetente(remetente);
        mensagem.setDestinatario(destinatario);
        mensagem.setTexto(texto.trim());
        return mensagem;
    }

    private Usuario buscarOutro(Usuario eu, Long outroId) {
        if (eu.getId().equals(outroId)) {
            throw new BusinessException("Você não pode enviar mensagens para a própria conta.");
        }
        return usuarioRepository.findById(outroId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + outroId));
    }

    private Usuario usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));
    }

    private MensagemResposta resposta(Mensagem m) {
        return new MensagemResposta(m.getId(), m.getRemetente().getId(), m.getDestinatario().getId(), m.getTexto(),
                m.getImagemArquivo() != null, m.getDataHora(), m.isLida());
    }

    /** Formatos aceitos, identificados pelo conteúdo do arquivo (não pelo nome nem pelo tipo informado). */
    private enum TipoImagem {
        PNG("image/png", ".png"),
        JPEG("image/jpeg", ".jpg"),
        GIF("image/gif", ".gif"),
        WEBP("image/webp", ".webp");

        private final String mime;
        private final String extensao;

        TipoImagem(String mime, String extensao) {
            this.mime = mime;
            this.extensao = extensao;
        }

        static TipoImagem detectar(byte[] b) {
            if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return PNG;
            }
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
                return GIF;
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W'
                    && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
