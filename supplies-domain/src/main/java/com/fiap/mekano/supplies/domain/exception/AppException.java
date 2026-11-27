package com.fiap.mekano.supplies.domain.exception;

/**
 * Excecao padrao da aplicacao. Carrega o status HTTP e uma mensagem.
 *
 * <p>O dominio nao importa classes de framework: o status e representado por
 * {@code int} e convertido para RFC 7807 na camada REST.</p>
 */
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
