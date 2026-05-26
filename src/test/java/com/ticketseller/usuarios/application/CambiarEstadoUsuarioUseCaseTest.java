package com.ticketseller.usuarios.application;

import com.ticketseller.usuarios.domain.exception.AutoCambioEstadoException;
import com.ticketseller.usuarios.domain.exception.UsuarioNotFoundException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoUsuarioUseCaseTest {

    @Mock
    private UsuarioRepositoryPort repositoryPort;

    private CambiarEstadoUsuarioUseCase useCase;

    private final UUID idAdmin = UUID.randomUUID();
    private final UUID idObjetivo = UUID.randomUUID();

    private final Usuario usuarioObjetivo = Usuario.builder()
            .id(idObjetivo)
            .nombre("Pedro")
            .email("pedro@test.com")
            .telefono("3001234567")
            .passwordHash("hash")
            .rol(RolUsuario.COMPRADOR)
            .estado(EstadoUsuario.ACTIVO)
            .fechaCreacion(LocalDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        useCase = new CambiarEstadoUsuarioUseCase(repositoryPort);
    }

    @Test
    void ejecutar_cuandoIdObjetivoEsElMismo_lanzaAutoCambioEstadoException() {
        StepVerifier.create(useCase.ejecutar(idAdmin, idAdmin, EstadoUsuario.INACTIVO))
                .expectError(AutoCambioEstadoException.class)
                .verify();
    }

    @Test
    void ejecutar_cuandoUsuarioNoExiste_lanzaUsuarioNotFoundException() {
        when(repositoryPort.buscarPorId(idObjetivo)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(idObjetivo, idAdmin, EstadoUsuario.BANNED))
                .expectError(UsuarioNotFoundException.class)
                .verify();
    }

    @Test
    void ejecutar_conDatosValidos_actualizaEstado() {
        when(repositoryPort.buscarPorId(idObjetivo)).thenReturn(Mono.just(usuarioObjetivo));
        when(repositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(idObjetivo, idAdmin, EstadoUsuario.BANNED))
                .assertNext(usuario -> assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.BANNED))
                .verifyComplete();
    }

    @Test
    void ejecutar_conDatosValidos_persisteNuevoEstado() {
        when(repositoryPort.buscarPorId(idObjetivo)).thenReturn(Mono.just(usuarioObjetivo));
        when(repositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(idObjetivo, idAdmin, EstadoUsuario.INACTIVO))
                .assertNext(usuario -> assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.INACTIVO))
                .verifyComplete();
    }
}
