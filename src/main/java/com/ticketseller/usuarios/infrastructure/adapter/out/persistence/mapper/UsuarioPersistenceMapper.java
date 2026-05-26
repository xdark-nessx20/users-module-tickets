package com.ticketseller.usuarios.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.usuarios.domain.model.EstadoUsuario;
import com.ticketseller.usuarios.domain.model.RolUsuario;
import com.ticketseller.usuarios.domain.model.Usuario;
import com.ticketseller.usuarios.infrastructure.adapter.out.persistence.UsuarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UsuarioPersistenceMapper {

    Usuario toDomain(UsuarioEntity entity);

    UsuarioEntity toEntity(Usuario domain);

    default RolUsuario toRolUsuario(String rol) {
        return rol != null ? RolUsuario.valueOf(rol) : null;
    }

    default EstadoUsuario toEstadoUsuario(String estado) {
        return estado != null ? EstadoUsuario.valueOf(estado) : null;
    }

    default String fromRolUsuario(RolUsuario rol) {
        return rol != null ? rol.name() : null;
    }

    default String fromEstadoUsuario(EstadoUsuario estado) {
        return estado != null ? estado.name() : null;
    }
}
