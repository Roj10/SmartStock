package com.smartstock.backend.controller;

import java.util.List;

import java.util.concurrent.TimeUnit;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartstock.backend.dto.MensagemDtos.Contato;
import com.smartstock.backend.dto.MensagemDtos.ImagemMensagem;
import com.smartstock.backend.dto.MensagemDtos.MensagemResposta;
import com.smartstock.backend.dto.MensagemDtos.NaoLidas;
import com.smartstock.backend.dto.MensagemRequest;
import com.smartstock.backend.service.MensagemService;

import jakarta.validation.Valid;

/** Conversas entre contas: qualquer usuário autenticado pode falar com os demais setores. */
@RestController
@RequestMapping("/api/mensagens")
public class MensagemController {

    private final MensagemService mensagemService;

    public MensagemController(MensagemService mensagemService) {
        this.mensagemService = mensagemService;
    }

    @GetMapping("/contatos")
    public List<Contato> contatos() {
        return mensagemService.listarContatos();
    }

    @GetMapping("/nao-lidas")
    public NaoLidas naoLidas() {
        return mensagemService.contarNaoLidas();
    }

    @GetMapping("/conversa/{usuarioId}")
    public List<MensagemResposta> conversa(@PathVariable Long usuarioId) {
        return mensagemService.abrirConversa(usuarioId);
    }

    @PostMapping(value = "/conversa/{usuarioId}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MensagemResposta enviarImagem(@PathVariable Long usuarioId, @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "texto", required = false) String texto) {
        return mensagemService.enviarImagem(usuarioId, texto, arquivo);
    }

    /** Imagem de uma mensagem: só é entregue a quem enviou ou recebeu (não há link público). */
    @GetMapping("/{mensagemId}/imagem")
    public ResponseEntity<Resource> imagem(@PathVariable Long mensagemId) {
        ImagemMensagem imagem = mensagemService.buscarImagem(mensagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.tipo()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .body(imagem.recurso());
    }

    @PostMapping("/conversa/{usuarioId}")
    public MensagemResposta enviar(@PathVariable Long usuarioId, @Valid @RequestBody MensagemRequest request) {
        return mensagemService.enviar(usuarioId, request.getTexto());
    }
}
