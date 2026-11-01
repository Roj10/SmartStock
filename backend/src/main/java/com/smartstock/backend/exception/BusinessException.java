package com.smartstock.backend.exception;

/**
 * <b>Em palavras simples:</b> Erro de "regra de negócio": o pedido estava bem escrito, mas o sistema não pode fazer aquilo
 * (ex.: saída maior que o estoque, senha do administrador errada). Vira a resposta 422 com a explicação.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
