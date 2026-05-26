package com.ticketseller.usuarios.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioEntity, UUID> {

    Mono<UsuarioEntity> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);
}
