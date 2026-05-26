package com.ticketseller.usuarios.infrastructure.config;

import com.ticketseller.usuarios.application.CambiarEstadoUsuarioUseCase;
import com.ticketseller.usuarios.application.LoginUsuarioUseCase;
import com.ticketseller.usuarios.application.RegistrarUsuarioUseCase;
import com.ticketseller.usuarios.domain.repository.UsuarioRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class BeanConfiguration {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepositoryPort repositoryPort,
                                                           BCryptPasswordEncoder passwordEncoder) {
        return new RegistrarUsuarioUseCase(repositoryPort, passwordEncoder);
    }

    @Bean
    public LoginUsuarioUseCase loginUsuarioUseCase(UsuarioRepositoryPort repositoryPort,
                                                   BCryptPasswordEncoder passwordEncoder,
                                                   JwtConfig jwtConfig) {
        return new LoginUsuarioUseCase(repositoryPort, passwordEncoder, jwtConfig);
    }

    @Bean
    public CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase(UsuarioRepositoryPort repositoryPort) {
        return new CambiarEstadoUsuarioUseCase(repositoryPort);
    }
}
