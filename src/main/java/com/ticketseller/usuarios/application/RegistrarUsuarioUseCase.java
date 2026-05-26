package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.EmailDuplicadoException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort repositoryPort;
    private final BCryptPasswordEncoder passwordEncoder;

    public Mono<Usuario> ejecutar(String nombre, String email, String telefono, String password, RolUsuario rol) {
        return repositoryPort.existePorEmail(email)
                .filter(existe -> !existe)
                .switchIfEmpty(Mono.error(new EmailDuplicadoException()))
                .flatMap(ignored -> {
                    String passwordHash = passwordEncoder.encode(password);
                    Usuario usuario = buildUsuario(nombre, email, telefono, passwordHash, rol);
                    usuario.validar();
                    return repositoryPort.guardar(usuario);
                });
    }

    private Usuario buildUsuario(String nombre, String email, String telefono, String passwordHash, RolUsuario rol) {
        return Usuario.builder()
                .nombre(nombre)
                .email(email)
                .telefono(telefono)
                .passwordHash(passwordHash)
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }
}
