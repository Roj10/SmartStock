package com.smartstock.backend.exception;

/** Muitas tentativas erradas seguidas (senha, código do 2FA ou senha do administrador): bloqueio temporário. */
public class MuitasTentativasException extends RuntimeException {
    public MuitasTentativasException(String message) {
        super(message);
    }
}
