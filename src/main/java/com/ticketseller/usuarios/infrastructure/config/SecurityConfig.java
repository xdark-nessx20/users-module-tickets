package com.ticketseller.usuarios.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(new JwtAuthenticationWebFilter(jwtConfig), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @RequiredArgsConstructor
    static class JwtAuthenticationWebFilter implements WebFilter {

        private final JwtConfig jwtConfig;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }
            String token = authHeader.substring(7);
            if (!jwtConfig.esValido(token)) {
                return chain.filter(exchange);
            }
            String sub = jwtConfig.extraerSub(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(sub, null, Collections.emptyList());
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }
    }
}
