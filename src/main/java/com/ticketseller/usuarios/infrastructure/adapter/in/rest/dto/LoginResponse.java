package com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto;

public record LoginResponse(
        String accessToken,
        long expiresIn
) {}
