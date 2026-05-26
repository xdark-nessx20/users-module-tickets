package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.AutoCambioEstadoException;
import com.ticketseller.usuarios.domain.exception.UsuarioNotFoundException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoUsuarioUseCase {

    private final UsuarioRepositoryPort repositoryPort;

    public Mono<Usuario> ejecutar(UUID idObjetivo, UUID idAutenticado, EstadoUsuario nuevoEstado) {
        if (idObjetivo.equals(idAutenticado)) {
            return Mono.error(new AutoCambioEstadoException());
        }
        return repositoryPort.buscarPorId(idObjetivo)
                .switchIfEmpty(Mono.error(new UsuarioNotFoundException()))
                .flatMap(usuario -> repositoryPort.guardar(usuario.conEstado(nuevoEstado)));
    }
}
