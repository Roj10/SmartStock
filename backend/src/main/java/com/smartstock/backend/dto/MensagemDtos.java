package com.smartstock.backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.core.io.Resource;

import com.smartstock.backend.model.Modulo;
import com.smartstock.backend.model.Role;

/** Respostas do módulo de conversas entre contas. Não expõem login nem senha de ninguém. */
public final class MensagemDtos {

    private MensagemDtos() {
    }

    public record MensagemResposta(Long id, Long remetenteId, Long destinatarioId, String texto,
            boolean temImagem, LocalDateTime dataHora, boolean lida) {
    }

    public record Contato(Long id, String nome, Role role, Set<Modulo> permissoes, MensagemResposta ultimaMensagem,
            long naoLidas) {
    }

    /** Arquivo de imagem de uma mensagem, com o tipo para a resposta HTTP. */
    public record ImagemMensagem(Resource recurso, String tipo) {
    }

    /** Total de mensagens não lidas e quem enviou a mais recente (para o aviso de nova mensagem). */
    public record NaoLidas(long total, String ultimoRemetente) {
    }
}
