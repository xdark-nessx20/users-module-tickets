package com.ticketseller.usuarios.domain.exception;

public class AutoCambioEstadoException extends RuntimeException {
    public AutoCambioEstadoException() {
        super("Un administrador no puede cambiar su propio estado");
    }
}
