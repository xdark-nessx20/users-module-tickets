package com.ticketseller.usuarios.domain.repository;

import com.ticketseller.usuarios.domain.model.Usuario;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsuarioRepositoryPort {

    Mono<Usuario> guardar(Usuario usuario);

    Mono<Usuario> buscarPorId(UUID id);

    Mono<Usuario> buscarPorEmail(String email);

    Mono<Boolean> existePorEmail(String email);
}
