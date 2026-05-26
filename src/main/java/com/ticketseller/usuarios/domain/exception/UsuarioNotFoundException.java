package com.ticketseller.usuarios.domain.exception;

public class UsuarioNotFoundException extends RuntimeException {
    public UsuarioNotFoundException() {
        super("Usuario no encontrado");
    }
}
