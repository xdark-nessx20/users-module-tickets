# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`users-module` is the identity microservice for the **TicketSeller** ecosystem. It handles user registration, JWT-based authentication, and user state management. It is a standalone service — not part of a monorepo.

**Stack**: Java 21, Spring Boot 4.0.6, Spring WebFlux (reactive), Spring Data R2DBC, Spring Security (reactive), PostgreSQL, jjwt 0.12.x, BCrypt, MapStruct, Lombok, SpringDoc OpenAPI 3.x.

## Commands

```bash
# Build
./gradlew build

# Run (dev profile, requires local PostgreSQL)
./gradlew bootRun

# Run tests (requires Docker for Testcontainers)
./gradlew test

# Run a single test class
./gradlew test --tests "com.ticketseller.usuarios.application.RegistrarUsuarioUseCaseTest"

# Run prod profile locally (requires a .env file)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

## Architecture

The codebase follows **hexagonal architecture** (ports and adapters), identical to all other services in the TicketSeller ecosystem:

```
src/main/java/com/ticketseller/usuarios/
├── domain/           ← Pure Java. Zero Spring/R2DBC/Jakarta imports allowed here.
│   ├── model/        ← Usuario, RolUsuario (enum), EstadoUsuario (enum)
│   ├── exception/    ← Domain exceptions (EmailDuplicadoException, etc.)
│   └── repository/   ← UsuarioRepositoryPort (output port interface)
├── application/      ← One use case class per operation (no @Component annotations)
│   ├── RegistrarUsuarioUseCase.java
│   ├── LoginUsuarioUseCase.java
│   └── CambiarEstadoUsuarioUseCase.java
└── infrastructure/
    ├── adapter/in/rest/         ← Controllers, DTOs, GlobalExceptionHandler, mappers
    ├── adapter/out/persistence/ ← UsuarioEntity, R2DBC repository, adapter, mappers
    └── config/                  ← BeanConfiguration, SecurityConfig, JwtConfig
```

**Key architectural rules**:
- Use cases have no `@Component`/`@Service` — they are instantiated via `BeanConfiguration.java`.
- BCrypt hashing happens inside `RegistrarUsuarioUseCase`, not in the controller or repository.
- `passwordHash` must never appear in any response DTO.
- Use `ServerSecurityContextHolder` (reactive, not servlet) to extract the authenticated user in controllers.
- `JwtAuthenticationWebFilter` in `SecurityConfig` populates the reactive `SecurityContext` before controllers run.

## Configuration & Profiles

Two Spring profiles exist:

- **dev** (`application.yml`) — direct values, safe to version; targets a local PostgreSQL instance.
- **prod** (`application-prod.yml`) — all sensitive properties as `${ENV_VAR}` references; no hardcoded values.

To run prod locally, copy `example.env` to `.env` (never commit `.env`) and set `SPRING_PROFILES_ACTIVE=prod`.

The JWT secret must never be hardcoded in source or versioned files, even in tests. The dev profile uses a clearly labeled non-production placeholder.

## Database

The `usuarios` table is created **manually** — there is no migration tool for production. For tests, the schema is initialized from `src/test/resources/schema.sql` via Testcontainers (PostgreSQL container).

There is no physical deletion of users (FR-007). State changes (`ACTIVO` / `INACTIVO` / `BANNED`) are the only lifecycle operations.

## Testing

Tests use **JUnit 5 + Testcontainers** (PostgreSQL). The `TestcontainersConfiguration` bean spins up a real PostgreSQL container via `@ServiceConnection` — no mocking of the database.

Three test layers mirror the hexagonal structure:
- `application/` — unit tests for use cases (mock the port)
- `infrastructure/adapter/in/rest/` — contract/controller tests
- `infrastructure/adapter/out/persistence/` — integration tests with Testcontainers

## Security Invariants

- Login returns HTTP 401 with an identical generic message whether the email does not exist or the password is wrong — never reveal email existence.
- Users with `INACTIVO` or `BANNED` state cannot obtain a JWT under any circumstance.
- The JWT payload contains: `sub` (UUID), `email`, `rol`, `exp`. The `rol` claim allows other microservices to authorize without querying this service.
- No refresh tokens in scope. Token invalidation is by expiration only.
- An administrator cannot change their own state (`AutoCambioEstadoException` → HTTP 409).
