package com.ticketseller.usuarios.domain.model;

import com.ticketseller.usuarios.domain.exception.DatosUsuarioInvalidosException;
import com.ticketseller.usuarios.domain.exception.TransicionEstadoInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private UUID id;
    private String nombre;
    private String email;
    private String telefono;
    private String passwordHash;
    private RolUsuario rol;
    private EstadoUsuario estado;
    private LocalDateTime fechaCreacion;

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public boolean estaInactivo() {
        return estado == EstadoUsuario.INACTIVO;
    }

    public boolean estaBaneado() {
        return estado == EstadoUsuario.BANNED;
    }

    public void desactivar() {
        if (!estaActivo()) {
            throw new TransicionEstadoInvalidaException("Solo se puede desactivar un usuario activo");
        }
        this.estado = EstadoUsuario.INACTIVO;
    }

    public void banear() {
        if (!estaActivo()) {
            throw new TransicionEstadoInvalidaException("Solo se puede banear un usuario activo");
        }
        this.estado = EstadoUsuario.BANNED;
    }

    public void reactivar() {
        if (!estaInactivo()) {
            throw new TransicionEstadoInvalidaException("Solo se puede reactivar un usuario inactivo");
        }
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void validar() {
        if (nombre == null || nombre.isBlank()) throw new DatosUsuarioInvalidosException("nombre");
        if (email == null || email.isBlank()) throw new DatosUsuarioInvalidosException("email");
        if (passwordHash == null || passwordHash.isBlank()) throw new DatosUsuarioInvalidosException("passwordHash");
        if (rol == null) throw new DatosUsuarioInvalidosException("rol");
    }
}
