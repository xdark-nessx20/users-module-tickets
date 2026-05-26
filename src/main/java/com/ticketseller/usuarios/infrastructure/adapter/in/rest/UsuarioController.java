package com.ticketseller.usuarios.infrastructure.adapter.in.rest;

import com.ticketseller.usuarios.application.CambiarEstadoUsuarioUseCase;
import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.CambiarEstadoRequest;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.mapper.UsuarioRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de estado de usuarios")
public class UsuarioController {

    private final CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;
    private final UsuarioRestMapper restMapper;

    @Operation(summary = "Cambiar estado de un usuario", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente")
    @ApiResponse(responseCode = "400", description = "Estado inválido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "No puede cambiar su propio estado")
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<UsuarioResponse>> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Authentication authentication) {

        EstadoUsuario estado;
        try {
            estado = EstadoUsuario.valueOf(request.estado());
        } catch (IllegalArgumentException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no reconocido"));
        }

        UUID idAutenticado = UUID.fromString(authentication.getName());
        return cambiarEstadoUsuarioUseCase.ejecutar(id, idAutenticado, estado)
                .map(usuario -> ResponseEntity.ok(restMapper.toResponse(usuario)));
    }
}
