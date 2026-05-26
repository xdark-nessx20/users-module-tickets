package com.ticketseller.usuarios.domain.exception;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }
}
