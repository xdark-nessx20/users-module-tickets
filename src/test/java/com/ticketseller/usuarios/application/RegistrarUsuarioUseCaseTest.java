package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.EmailDuplicadoException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepositoryPort repositoryPort;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private RegistrarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarUsuarioUseCase(repositoryPort, passwordEncoder);
    }

    @Test
    void ejecutar_conEmailExistente_lanzaEmailDuplicadoException() {
        when(repositoryPort.existePorEmail("test@test.com")).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar("Juan", "test@test.com", "1234567", "password", RolUsuario.COMPRADOR))
                .expectError(EmailDuplicadoException.class)
                .verify();
    }

    @Test
    void ejecutar_conDatosValidos_hashesPasswordYAsignaEstadoActivo() {
        when(repositoryPort.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("mi-password")).thenReturn("hashed-password");
        when(repositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar("Juan", "juan@test.com", "3001234567", "mi-password", RolUsuario.COMPRADOR))
                .assertNext(usuario -> {
                    assertThat(usuario.getPasswordHash()).isEqualTo("hashed-password");
                    assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
                    assertThat(usuario.getFechaCreacion()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void ejecutar_conDatosValidos_persisteConHashCorrecto() {
        when(repositoryPort.existePorEmail(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(repositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        useCase.ejecutar("Ana", "ana@test.com", "3007654321", "secret", RolUsuario.AGENTE_VENTAS)
                .block();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }
}
