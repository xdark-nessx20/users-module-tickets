package com.ticketseller.usuarios.infrastructure.adapter.out.persistence;

import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import com.ticketseller.usuarios.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioR2dbcRepository r2dbcRepository;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Mono<Usuario> guardar(Usuario usuario) {
        return r2dbcRepository.save(mapper.toEntity(usuario))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Usuario> buscarPorId(UUID id) {
        return r2dbcRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Usuario> buscarPorEmail(String email) {
        return r2dbcRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existePorEmail(String email) {
        return r2dbcRepository.existsByEmail(email);
    }
}
