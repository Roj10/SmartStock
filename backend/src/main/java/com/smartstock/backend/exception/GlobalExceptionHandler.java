package com.smartstock.backend.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoAutenticado(NaoAutenticadoException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<Map<String, Object>> handleTentativas(MuitasTentativasException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAcessoNegado(Exception ex) {
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para esta ação.");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAutenticacao(Exception ex) {
        return build(HttpStatus.UNAUTHORIZED, "Sessão expirada ou inválida. Entre de novo.");
    }

    /** Excluir/alterar algo que outros cadastros usam (ex.: fornecedor com itens, item com movimentações). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(Exception ex) {
        return build(HttpStatus.CONFLICT, "Não foi possível concluir: este registro está ligado a outros cadastros "
                + "(movimentações, itens, projetos...).");
    }

    /**
     * Qualquer outro erro: erros conhecidos do Spring mantêm o código HTTP; o resto vira 500 com mensagem genérica
     * (o detalhe vai só para o log do servidor, nunca para quem chamou).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.BAD_REQUEST;
            }
            return build(status, status.is4xxClientError() ? "Requisição inválida (" + status.value() + ")."
                    : "Erro no servidor. Tente de novo.");
        }
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            return build(HttpStatus.BAD_REQUEST, "Requisição inválida.");
        }
        log.error("Erro inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro no servidor. Tente de novo.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadGrande(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo é grande demais. O limite é de 20 MB por arquivo (5 MB para imagens).");
    }

    /** Falha ao ler/gravar arquivos (por exemplo, sem conexão com o Supabase Storage). */
    @ExceptionHandler(java.io.UncheckedIOException.class)
    public ResponseEntity<Map<String, Object>> handleArquivo(java.io.UncheckedIOException ex) {
        // o detalhe (resposta do Supabase) fica só no log do servidor
        log.warn("Falha ao acessar arquivos: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Não foi possível acessar os arquivos agora. Tente de novo.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
