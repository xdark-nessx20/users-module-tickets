package com.ticketseller.usuarios.infrastructure.adapter.in.rest;

import com.ticketseller.usuarios.application.LoginUsuarioUseCase;
import com.ticketseller.usuarios.application.RegistrarUsuarioUseCase;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.LoginRequest;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.LoginResponse;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.RegistroRequest;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import com.ticketseller.usuarios.infrastructure.config.JwtConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro y login de usuarios")
public class AuthController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final LoginUsuarioUseCase loginUsuarioUseCase;
    private final UsuarioRestMapper restMapper;
    private final JwtConfig jwtConfig;

    @Operation(summary = "Registrar nuevo usuario")
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "409", description = "Email ya registrado")
    @PostMapping("/registro")
    public Mono<ResponseEntity<UsuarioResponse>> registro(@Valid @RequestBody RegistroRequest request) {
        RolUsuario rol = RolUsuario.valueOf(request.rol());
        return registrarUsuarioUseCase.ejecutar(
                        request.nombre(), request.email(), request.telefono(), request.password(), rol)
                .map(usuario -> ResponseEntity.status(HttpStatus.CREATED).body(restMapper.toResponse(usuario)));
    }

    @Operation(summary = "Autenticar usuario")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    @ApiResponse(responseCode = "403", description = "Cuenta inactiva o suspendida")
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return loginUsuarioUseCase.ejecutar(request.email(), request.password())
                .map(token -> ResponseEntity.ok(new LoginResponse(token, jwtConfig.getExpirationMs())));
    }
}
