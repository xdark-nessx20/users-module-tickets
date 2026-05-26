package com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(

        @NotBlank(message = "El estado es obligatorio")
        String estado
) {}
