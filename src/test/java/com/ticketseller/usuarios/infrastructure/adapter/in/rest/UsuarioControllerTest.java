package com.ticketseller.usuarios.infrastructure.adapter.in.rest;

import com.ticketseller.usuarios.application.CambiarEstadoUsuarioUseCase;
import com.ticketseller.usuarios.domain.exception.AutoCambioEstadoException;
import com.ticketseller.usuarios.domain.exception.UsuarioNotFoundException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.infrastructure.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.r2dbc.pool.initial-size=0")
class UsuarioControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtConfig jwtConfig;

    @MockitoBean
    private CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;

    private WebTestClient webTestClient;

    private final UUID adminId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID objetivoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private String adminToken;

    private final Usuario usuarioActualizado = Usuario.builder()
            .id(objetivoId)
            .nombre("Pedro")
            .email("pedro@test.com")
            .telefono("3001234567")
            .passwordHash("hash")
            .rol(RolUsuario.COMPRADOR)
            .estado(EstadoUsuario.BANNED)
            .fechaCreacion(LocalDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        Usuario adminUser = Usuario.builder()
                .id(adminId)
                .nombre("Admin")
                .email("admin@test.com")
                .telefono("3001234567")
                .passwordHash("hash")
                .rol(RolUsuario.ADMINISTRADOR_RECINTO)
                .estado(EstadoUsuario.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();
        adminToken = jwtConfig.generarToken(adminUser);
    }

    @Test
    void cambiarEstado_conTokenValidoYEstadoValido_retornaHttp200() {
        when(cambiarEstadoUsuarioUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.just(usuarioActualizado));

        webTestClient.patch().uri("/api/v1/usuarios/{id}/estado", objetivoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"estado":"BANNED"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("BANNED");
    }

    @Test
    void cambiarEstado_sinToken_retornaHttp401() {
        webTestClient.patch().uri("/api/v1/usuarios/{id}/estado", objetivoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"estado":"BANNED"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void cambiarEstado_conUUIDInexistente_retornaHttp404() {
        when(cambiarEstadoUsuarioUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.error(new UsuarioNotFoundException()));

        webTestClient.patch().uri("/api/v1/usuarios/{id}/estado", objetivoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"estado":"BANNED"}
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Usuario no encontrado");
    }

    @Test
    void cambiarEstado_administradorCambiaPropiEstado_retornaHttp409() {
        when(cambiarEstadoUsuarioUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.error(new AutoCambioEstadoException()));

        webTestClient.patch().uri("/api/v1/usuarios/{id}/estado", adminId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"estado":"INACTIVO"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Un administrador no puede cambiar su propio estado");
    }
}
