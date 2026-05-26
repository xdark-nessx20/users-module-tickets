package com.ticketseller.usuarios.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {

    @Id
    private UUID id;

    private String nombre;

    private String email;

    private String telefono;

    @Column("password_hash")
    private String passwordHash;

    private String rol;

    private String estado;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
}
