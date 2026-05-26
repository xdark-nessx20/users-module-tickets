package com.ticketseller.usuarios.domain.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException() {
        super("Ya existe un usuario registrado con ese email");
    }
}
