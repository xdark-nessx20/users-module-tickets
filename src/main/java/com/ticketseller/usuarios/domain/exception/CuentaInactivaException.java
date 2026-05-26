package com.ticketseller.usuarios.domain.exception;

public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException() {
        super("Cuenta inactiva");
    }
}
