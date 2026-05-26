package com.ticketseller.usuarios.infrastructure.config;

import com.ticketseller.usuarios.domain.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtConfig {

    private String secret;
    private long expirationMs;

    public String generarToken(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    public String extraerEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String extraerSub(String token) {
        return getClaims(token).getSubject();
    }

    public boolean esValido(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private io.jsonwebtoken.Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
