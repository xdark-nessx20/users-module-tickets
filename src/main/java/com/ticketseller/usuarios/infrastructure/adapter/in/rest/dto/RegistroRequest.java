package com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato válido")
        String email,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe contener solo dígitos (7-15 caracteres)")
        String telefono,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {}
