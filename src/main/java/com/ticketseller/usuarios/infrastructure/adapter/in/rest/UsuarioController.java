package com.ticketseller.usuarios.infrastructure.adapter.in.rest;

import com.ticketseller.usuarios.application.CambiarEstadoUsuarioUseCase;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de estado de usuarios")
public class UsuarioController {

    private final CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;
    private final UsuarioRestMapper restMapper;

    @Operation(summary = "Desactivar un usuario", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "No puede cambiar su propio estado")
    @PatchMapping("/{id}/desactivar")
    public Mono<ResponseEntity<UsuarioResponse>> desactivar(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID idAutenticado = UUID.fromString(authentication.getName());
        return cambiarEstadoUsuarioUseCase.ejecutar(id, idAutenticado, EstadoUsuario.INACTIVO)
                .map(usuario -> ResponseEntity.ok(restMapper.toResponse(usuario)));
    }

    @Operation(summary = "Reactivar un usuario", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Usuario reactivado exitosamente")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "No puede cambiar su propio estado")
    @PatchMapping("/{id}/reactivar")
    public Mono<ResponseEntity<UsuarioResponse>> reactivar(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID idAutenticado = UUID.fromString(authentication.getName());
        return cambiarEstadoUsuarioUseCase.ejecutar(id, idAutenticado, EstadoUsuario.ACTIVO)
                .map(usuario -> ResponseEntity.ok(restMapper.toResponse(usuario)));
    }

    @Operation(summary = "Banear un usuario", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Usuario baneado exitosamente")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "No puede cambiar su propio estado")
    @PatchMapping("/{id}/banear")
    public Mono<ResponseEntity<UsuarioResponse>> banear(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID idAutenticado = UUID.fromString(authentication.getName());
        return cambiarEstadoUsuarioUseCase.ejecutar(id, idAutenticado, EstadoUsuario.BANNED)
                .map(usuario -> ResponseEntity.ok(restMapper.toResponse(usuario)));
    }
}
