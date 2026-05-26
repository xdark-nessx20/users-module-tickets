package com.ticketseller.usuarios.infrastructure.adapter.in.rest;

import com.ticketseller.usuarios.application.LoginUsuarioUseCase;
import com.ticketseller.usuarios.application.RegistrarUsuarioUseCase;
import com.ticketseller.usuarios.domain.exception.CredencialesInvalidasException;
import com.ticketseller.usuarios.domain.exception.CuentaBanneadaException;
import com.ticketseller.usuarios.domain.exception.CuentaInactivaException;
import com.ticketseller.usuarios.domain.exception.EmailDuplicadoException;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.r2dbc.pool.initial-size=0")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @MockitoBean
    private LoginUsuarioUseCase loginUsuarioUseCase;

    private WebTestClient webTestClient;

    private final Usuario usuarioEjemplo = Usuario.builder()
            .id(UUID.randomUUID())
            .nombre("Juan")
            .email("juan@test.com")
            .telefono("3001234567")
            .passwordHash("hashed")
            .rol(RolUsuario.COMPRADOR)
            .estado(EstadoUsuario.ACTIVO)
            .fechaCreacion(LocalDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void registro_conDatosValidos_retornaHttp201SinPasswordHash() {
        when(registrarUsuarioUseCase.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(usuarioEjemplo));

        webTestClient.post().uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nombre":"Juan","email":"juan@test.com","telefono":"3001234567",
                         "password":"password123","rol":"COMPRADOR"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.email").isEqualTo("juan@test.com")
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void registro_conEmailDuplicado_retornaHttp409() {
        when(registrarUsuarioUseCase.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new EmailDuplicadoException()));

        webTestClient.post().uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nombre":"Juan","email":"juan@test.com","telefono":"3001234567",
                         "password":"password123","rol":"COMPRADOR"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Ya existe un usuario registrado con ese email");
    }

    @Test
    void registro_conCamposVacios_retornaHttp400() {
        webTestClient.post().uri("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"nombre":"","email":"","telefono":"","password":"","rol":""}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_conCredencialesValidas_retornaHttp200ConToken() {
        when(loginUsuarioUseCase.ejecutar(anyString(), anyString())).thenReturn(Mono.just("jwt-token"));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"juan@test.com","password":"password123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("jwt-token")
                .jsonPath("$.expiresIn").exists();
    }

    @Test
    void login_conPasswordIncorrecto_retornaHttp401ConMensajeGenerico() {
        when(loginUsuarioUseCase.ejecutar(anyString(), anyString()))
                .thenReturn(Mono.error(new CredencialesInvalidasException()));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"juan@test.com","password":"wrong"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Credenciales inválidas");
    }

    @Test
    void login_conEmailInexistente_retornaHttp401ConMismoMensajeGenerico() {
        when(loginUsuarioUseCase.ejecutar(anyString(), anyString()))
                .thenReturn(Mono.error(new CredencialesInvalidasException()));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"noexiste@test.com","password":"pass"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Credenciales inválidas");
    }

    @Test
    void login_conUsuarioInactivo_retornaHttp403() {
        when(loginUsuarioUseCase.ejecutar(anyString(), anyString()))
                .thenReturn(Mono.error(new CuentaInactivaException()));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"inactivo@test.com","password":"pass"}
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Cuenta inactiva");
    }

    @Test
    void login_conUsuarioBanned_retornaHttp403() {
        when(loginUsuarioUseCase.ejecutar(anyString(), anyString()))
                .thenReturn(Mono.error(new CuentaBanneadaException()));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"banned@test.com","password":"pass"}
                        """)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Cuenta suspendida");
    }
}
