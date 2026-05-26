package com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nombre,
        String email,
        String telefono,
        String rol,
        String estado,
        LocalDateTime fechaCreacion
) {}
