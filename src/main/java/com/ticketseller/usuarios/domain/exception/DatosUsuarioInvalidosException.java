package com.ticketseller.usuarios.domain.exception;

public class DatosUsuarioInvalidosException extends RuntimeException {
    public DatosUsuarioInvalidosException(String campo) {
        super("El campo '" + campo + "' es obligatorio");
    }
}
