package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.CredencialesInvalidasException;
import com.ticketseller.usuarios.domain.exception.CuentaBanneadaException;
import com.ticketseller.usuarios.domain.exception.CuentaInactivaException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import com.ticketseller.usuarios.infrastructure.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUsuarioUseCaseTest {

    @Mock
    private UsuarioRepositoryPort repositoryPort;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    private LoginUsuarioUseCase useCase;

    private final UUID usuarioId = UUID.randomUUID();

    private final Usuario usuarioActivo = Usuario.builder()
            .id(usuarioId)
            .email("activo@test.com")
            .passwordHash("hashed")
            .rol(RolUsuario.COMPRADOR)
            .estado(EstadoUsuario.ACTIVO)
            .fechaCreacion(LocalDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        useCase = new LoginUsuarioUseCase(repositoryPort, passwordEncoder, jwtConfig);
    }

    @Test
    void ejecutar_conEmailInexistente_lanzaCredencialesInvalidas() {
        when(repositoryPort.buscarPorEmail(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar("noexiste@test.com", "pass"))
                .expectError(CredencialesInvalidasException.class)
                .verify();
    }

    @Test
    void ejecutar_conPasswordIncorrecto_lanzaCredencialesInvalidas() {
        when(repositoryPort.buscarPorEmail("activo@test.com")).thenReturn(Mono.just(usuarioActivo));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        StepVerifier.create(useCase.ejecutar("activo@test.com", "wrong"))
                .expectError(CredencialesInvalidasException.class)
                .verify();
    }

    @Test
    void ejecutar_conUsuarioInactivo_lanzaCuentaInactiva() {
        Usuario inactivo = usuarioActivo.conEstado(EstadoUsuario.INACTIVO);
        when(repositoryPort.buscarPorEmail("activo@test.com")).thenReturn(Mono.just(inactivo));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        StepVerifier.create(useCase.ejecutar("activo@test.com", "pass"))
                .expectError(CuentaInactivaException.class)
                .verify();
    }

    @Test
    void ejecutar_conUsuarioBanned_lanzaCuentaBanneada() {
        Usuario banned = usuarioActivo.conEstado(EstadoUsuario.BANNED);
        when(repositoryPort.buscarPorEmail("activo@test.com")).thenReturn(Mono.just(banned));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);

        StepVerifier.create(useCase.ejecutar("activo@test.com", "pass"))
                .expectError(CuentaBanneadaException.class)
                .verify();
    }

    @Test
    void ejecutar_conCredencialesValidas_retornaToken() {
        when(repositoryPort.buscarPorEmail("activo@test.com")).thenReturn(Mono.just(usuarioActivo));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtConfig.generarToken(usuarioActivo)).thenReturn("jwt-token");

        StepVerifier.create(useCase.ejecutar("activo@test.com", "pass"))
                .expectNext("jwt-token")
                .verifyComplete();
    }
}
