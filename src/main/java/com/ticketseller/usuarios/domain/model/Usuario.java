package com.ticketseller.usuarios.domain.model;

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

    public Usuario conEstado(EstadoUsuario nuevoEstado) {
        return Usuario.builder()
                .id(this.id)
                .nombre(this.nombre)
                .email(this.email)
                .telefono(this.telefono)
                .passwordHash(this.passwordHash)
                .rol(this.rol)
                .estado(nuevoEstado)
                .fechaCreacion(this.fechaCreacion)
                .build();
    }
}
