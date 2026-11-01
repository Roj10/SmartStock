package com.smartstock.backend.exception;

/**
 * <b>Em palavras simples:</b> Erro de "não encontrado": o registro pedido (item, projeto, conta...) não existe. Vira a resposta 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
