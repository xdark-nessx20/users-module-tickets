package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.CredencialesInvalidasException;
import com.ticketseller.usuarios.domain.exception.CuentaBanneadaException;
import com.ticketseller.usuarios.domain.exception.CuentaInactivaException;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import com.ticketseller.usuarios.infrastructure.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoginUsuarioUseCase {

    private final UsuarioRepositoryPort repositoryPort;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public Mono<String> ejecutar(String email, String password) {
        return repositoryPort.buscarPorEmail(email)
                .switchIfEmpty(Mono.error(new CredencialesInvalidasException()))
                .filter(usuario -> passwordCoincide(password, usuario))
                .switchIfEmpty(Mono.error(new CredencialesInvalidasException()))
                .flatMap(this::generarTokenSegunEstado);
    }

    private Mono<String> generarTokenSegunEstado(Usuario usuario) {
        return switch (usuario.getEstado()) {
            case INACTIVO -> Mono.error(new CuentaInactivaException());
            case BANNED -> Mono.error(new CuentaBanneadaException());
            default -> Mono.just(jwtConfig.generarToken(usuario));
        };
    }

    private boolean passwordCoincide(String password, Usuario usuario) {
        return passwordEncoder.matches(password, usuario.getPasswordHash());
    }
}
