package com.smartstock.backend.exception;

/** Login recusado (usuário/senha errados ou etapa de login vencida): responde 401. */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String message) {
        super(message);
    }
}
