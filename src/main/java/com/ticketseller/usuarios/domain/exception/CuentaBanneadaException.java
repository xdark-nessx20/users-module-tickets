package com.ticketseller.usuarios.domain.exception;

public class CuentaBanneadaException extends RuntimeException {
    public CuentaBanneadaException() {
        super("Cuenta suspendida");
    }
}
