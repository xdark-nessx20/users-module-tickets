package com.ticketseller.usuarios.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.infrastructure.adapter.in.rest.dto.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UsuarioRestMapper {

    UsuarioResponse toResponse(Usuario usuario);

    default String map(RolUsuario rol) {
        return rol != null ? rol.name() : null;
    }

    default String map(EstadoUsuario estado) {
        return estado != null ? estado.name() : null;
    }
}
