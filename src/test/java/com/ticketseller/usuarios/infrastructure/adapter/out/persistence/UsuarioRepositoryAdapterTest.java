package com.ticketseller.usuarios.infrastructure.adapter.out.persistence;

import com.ticketseller.usuarios.TestcontainersConfiguration;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext
class UsuarioRepositoryAdapterTest {

    @Autowired
    private UsuarioRepositoryPort repositoryPort;

    private Usuario usuarioEjemplo(String email) {
        return Usuario.builder()
                .nombre("Test User")
                .email(email)
                .telefono("3001234567")
                .passwordHash("$2a$10$hashed.password.value.here.bcrypt")
                .rol(RolUsuario.COMPRADOR)
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    void guardar_nuevoUsuario_asignaIdGeneradoPorBaseDeDatos() {
        Usuario usuario = usuarioEjemplo("nuevo@test.com");

        StepVerifier.create(repositoryPort.guardar(usuario))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getEmail()).isEqualTo("nuevo@test.com");
                    assertThat(saved.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
                })
                .verifyComplete();
    }

    @Test
    void buscarPorEmail_conEmailExistente_retornaUsuario() {
        Usuario usuario = usuarioEjemplo("buscar@test.com");
        repositoryPort.guardar(usuario).block();

        StepVerifier.create(repositoryPort.buscarPorEmail("buscar@test.com"))
                .assertNext(found -> assertThat(found.getEmail()).isEqualTo("buscar@test.com"))
                .verifyComplete();
    }

    @Test
    void buscarPorEmail_conEmailInexistente_retornaVacio() {
        StepVerifier.create(repositoryPort.buscarPorEmail("noexiste@test.com"))
                .verifyComplete();
    }

    @Test
    void existePorEmail_conEmailExistente_retornaTrue() {
        repositoryPort.guardar(usuarioEjemplo("existe@test.com")).block();

        StepVerifier.create(repositoryPort.existePorEmail("existe@test.com"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existePorEmail_conEmailInexistente_retornaFalse() {
        StepVerifier.create(repositoryPort.existePorEmail("noexiste_tampoco@test.com"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void buscarPorId_conIdExistente_retornaUsuario() {
        Usuario guardado = repositoryPort.guardar(usuarioEjemplo("porid@test.com")).block();
        UUID id = guardado.getId();

        StepVerifier.create(repositoryPort.buscarPorId(id))
                .assertNext(found -> {
                    assertThat(found.getId()).isEqualTo(id);
                    assertThat(found.getRol()).isEqualTo(RolUsuario.COMPRADOR);
                })
                .verifyComplete();
    }

    @Test
    void guardar_usuarioExistente_actualizaEstado() {
        Usuario original = repositoryPort.guardar(usuarioEjemplo("actualizar@test.com")).block();
        Usuario actualizado = original.banear();

        StepVerifier.create(repositoryPort.guardar(actualizado))
                .assertNext(saved -> assertThat(saved.getEstado()).isEqualTo(EstadoUsuario.BANNED))
                .verifyComplete();
    }
}
