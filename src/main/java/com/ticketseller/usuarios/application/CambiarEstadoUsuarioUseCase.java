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
        if (idCoincide(idAutenticado, idObjetivo)) {
            return Mono.error(new AutoCambioEstadoException());
        }
        return repositoryPort.buscarPorId(idObjetivo)
                .switchIfEmpty(Mono.error(new UsuarioNotFoundException()))
                .flatMap(usuario -> repositoryPort.guardar(aplicarTransicion(usuario, nuevoEstado)));
    }

    private Usuario aplicarTransicion(Usuario usuario, EstadoUsuario nuevoEstado) {
        switch (nuevoEstado) {
            case INACTIVO -> usuario.desactivar();
            case BANNED -> usuario.banear();
            case ACTIVO -> usuario.reactivar();
        };
        return usuario;
    }

    private boolean idCoincide(UUID idAutenticado, UUID idObjetivo) {
        return idObjetivo.equals(idAutenticado);
    }
}
