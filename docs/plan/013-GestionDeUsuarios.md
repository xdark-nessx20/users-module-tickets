# Implementation Plan: Gestión de Usuarios

**Date**: 26/05/2026  
**Specs**:

- [016-GestionDeUsuarios.md](/docs/spec/016-GestionDeUsuarios.md)

## Summary

El microservicio de **Gestión de Usuarios** es el servicio de identidad del ecosistema TicketSeller. Expone
endpoints para registro, autenticación y administración de estado de usuarios. La autenticación se basa en
**JWT (JSON Web Tokens)** firmados con una clave secreta, gestionada vía Spring Security.

La arquitectura es hexagonal (puertos y adaptadores), igual que el resto del ecosistema: dominio puro sin
dependencias externas, casos de uso en `application/`, adaptadores REST y de persistencia en
`infrastructure/`. La seguridad se configura como un adaptador de infraestructura que no contamina el dominio.

La configuración de entornos se divide en dos perfiles: `dev` (valores locales directos en `application.yml`)
y `prod` (todas las propiedades sensibles inyectadas como variables de entorno desde un archivo `.env`).

---

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Spring WebFlux, Spring Data R2DBC, Spring Security (reactive),
`jjwt` (io.jsonwebtoken) 0.12.x, BCrypt (via Spring Security), Bean Validation (Jakarta), MapStruct 1.5.5,
Lombok 1.18.40, SpringDoc OpenAPI 2.x  
**Storage**: PostgreSQL — esquema creado manualmente  
**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (PostgreSQL)  
**Target Platform**: Microservicio independiente — no forma parte del monorrepo principal de TicketSeller  
**Project Type**: Web (API REST reactiva con WebFlux)  
**Performance Goals**: Registro < 500ms (SC-001), login < 300ms (SC-002)  
**Constraints**: Sin borrado físico de usuarios (FR-007). Sin refresh tokens en este alcance. La clave JWT
jamás debe estar hardcodeada en código fuente ni en archivos versionados en prod.  
**Scale/Scope**: Microservicio de identidad — prerequisito para cualquier otro servicio que requiera
autenticación en el ecosistema.

---

## Environment Configuration

### Perfiles de Spring

El proyecto mantiene **dos archivos de configuración**:

```
src/main/resources/
├── application.yml          ← Perfil dev (valores directos, seguro para versionar)
└── application-prod.yml     ← Perfil prod (todas las propiedades sensibles como ${VAR})
```

**`application.yml` (dev)** — valores directos para desarrollo y pruebas locales:

```yaml
spring:
  profiles:
    active: dev
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/usuarios_dev
    username: postgres
    password: postgres
  security:
    ignored: /api/auth/**

app:
  jwt:
    secret: dev-secret-key-not-for-production-use-only
    expiration-ms: 86400000  # 24h

server:
  port: 8081
```

**`application-prod.yml`** — todas las propiedades sensibles como variables de entorno:

```yaml
spring:
  r2dbc:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS}

server:
  port: ${SERVER_PORT:8081}
```

Para activar el perfil prod: `SPRING_PROFILES_ACTIVE=prod` en el entorno de ejecución.

### Archivo `.env` (local, nunca versionado)

Un archivo `.env` en la raíz del proyecto carga las variables de entorno para el perfil `prod` en
desarrollo local o en el servidor. **Debe estar en `.gitignore`**.

```
# .gitignore debe incluir:
.env
```

### Archivo `example.env` (versionado, sirve de guía)

```
# example.env — copiar como .env y completar con valores reales

# Base de datos
DB_URL=r2dbc:postgresql://localhost:5432/usuarios_prod
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=replace-with-a-strong-random-secret-min-256-bits
JWT_EXPIRATION_MS=86400000

# Servidor
SERVER_PORT=8081
```

---

## Project Structure

### Documentation (this feature)

```text
docs/
├── spec/016-GestionDeUsuarios.md
└── plan/013-GestionDeUsuarios.md     # Este archivo
```

### Clases nuevas que agrega este feature

```text
src/main/java/com/ticketseller/usuarios/
├── domain/
│   ├── model/
│   │   ├── Usuario.java
│   │   ├── RolUsuario.java
│   │   └── EstadoUsuario.java
│   ├── exception/
│   │   ├── EmailDuplicadoException.java
│   │   ├── UsuarioNotFoundException.java
│   │   ├── CredencialesInvalidasException.java
│   │   ├── CuentaInactivaException.java
│   │   ├── CuentaBanneadaException.java
│   │   └── AutoCambioEstadoException.java
│   └── repository/
│       └── UsuarioRepositoryPort.java
├── application/
│   ├── RegistrarUsuarioUseCase.java
│   ├── LoginUsuarioUseCase.java
│   └── CambiarEstadoUsuarioUseCase.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── AuthController.java
    │   ├── UsuarioController.java
    │   ├── GlobalExceptionHandler.java
    │   ├── ApiErrorResponse.java
    │   └── dto/
    │       ├── RegistroRequest.java
    │       ├── LoginRequest.java
    │       ├── LoginResponse.java
    │       ├── CambiarEstadoRequest.java
    │       └── UsuarioResponse.java
    ├── adapter/in/rest/mapper/
    │   └── UsuarioRestMapper.java
    ├── adapter/out/persistence/
    │   ├── UsuarioEntity.java
    │   ├── UsuarioR2dbcRepository.java
    │   ├── UsuarioRepositoryAdapter.java
    │   └── mapper/
    │       └── UsuarioPersistenceMapper.java
    └── config/
        ├── BeanConfiguration.java
        ├── SecurityConfig.java
        └── JwtConfig.java

src/main/resources/
├── application.yml
└── application-prod.yml

src/test/java/com/ticketseller/usuarios/
├── application/
│   ├── RegistrarUsuarioUseCaseTest.java
│   ├── LoginUsuarioUseCaseTest.java
│   └── CambiarEstadoUsuarioUseCaseTest.java
└── infrastructure/
    ├── adapter/in/rest/
    │   ├── AuthControllerTest.java
    │   └── UsuarioControllerTest.java
    └── adapter/out/persistence/
        └── UsuarioRepositoryAdapterTest.java
```

**Structure Decision**: Arquitectura hexagonal idéntica al resto del ecosistema TicketSeller. `domain/`
contiene únicamente el modelo, enums y puertos — Java puro, cero dependencias externas. `application/`
tiene un caso de uso por operación. `infrastructure/` concentra Spring Security, JWT, R2DBC y los
controladores REST. La configuración de seguridad vive en `infrastructure/config/` y nunca toca el dominio.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto y estructura base del microservicio

- [ ] T001 Crear proyecto Spring Boot 3.x con Java 21 usando Spring Initializr. Dependencias: Spring
  WebFlux, Spring Data R2DBC, Spring Security, Bean Validation, R2DBC PostgreSQL Driver, Lombok,
  MapStruct, Testcontainers, SpringDoc OpenAPI 2.x, jjwt-api + jjwt-impl + jjwt-jackson 0.12.x
- [ ] T002 Crear estructura de paquetes hexagonal completa según el layout definido arriba
- [ ] T003 Crear `application.yml` con conexión R2DBC al perfil dev (PostgreSQL local) y propiedades
  `app.jwt.secret` y `app.jwt.expiration-ms` con valores de desarrollo
- [ ] T004 Crear `application-prod.yml` con todas las propiedades sensibles referenciadas como
  `${VARIABLE_ENTORNO}` (sin valores hardcodeados)
- [ ] T005 Crear `example.env` en la raíz del proyecto con todas las variables necesarias y comentarios
  descriptivos; agregar `.env` al `.gitignore`
- [ ] T006 Configurar Checkstyle para linting de código Java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Núcleo de dominio e infraestructura base. Debe completarse antes de cualquier user story.

**⚠️ CRITICAL**: Ninguna user story puede comenzar hasta que esta fase esté completa.

- [ ] T007 Crear tabla `usuarios` manualmente en PostgreSQL:
  ```sql
  CREATE TABLE usuarios (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre       VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    telefono     VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol          VARCHAR(50)  NOT NULL,
    estado       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP   NOT NULL DEFAULT now()
  );
  ```
- [ ] T008 Crear clases de dominio en `domain/model/`:
  - `Usuario.java`: `id` (UUID), `nombre` (String), `email` (String), `telefono` (String),
    `passwordHash` (String), `rol` (RolUsuario), `estado` (EstadoUsuario), `fechaCreacion` (LocalDateTime)
  - `RolUsuario.java` (enum): `ADMINISTRADOR_RECINTO`, `GESTOR_INVENTARIO`, `COORDINADOR_PATROCINIOS`,
    `AGENTE_VENTAS`, `COMPRADOR`, `PROMOTOR_EVENTOS`, `CONTROLADOR_ACCESOS`, `ADMINISTRADOR_FINANCIERO`
  - `EstadoUsuario.java` (enum): `ACTIVO`, `INACTIVO`, `BANNED`
- [ ] T009 Crear excepciones de dominio en `domain/exception/`: `EmailDuplicadoException`,
  `UsuarioNotFoundException`, `CredencialesInvalidasException`, `CuentaInactivaException`,
  `CuentaBanneadaException`, `AutoCambioEstadoException`
- [ ] T010 Crear interfaz de puerto de salida `UsuarioRepositoryPort.java` en `domain/repository/`
  con métodos:
  - `guardar(Usuario): Mono<Usuario>`
  - `buscarPorId(UUID): Mono<Usuario>`
  - `buscarPorEmail(String): Mono<Usuario>`
  - `existePorEmail(String): Mono<Boolean>`
- [ ] T011 Crear `UsuarioEntity.java` con anotaciones `@Table("usuarios")` y mapeo de todas las columnas
- [ ] T012 Crear `UsuarioR2dbcRepository.java` extendiendo `ReactiveCrudRepository<UsuarioEntity, UUID>`
  con método `findByEmail(String): Mono<UsuarioEntity>`
- [ ] T013 Implementar `UsuarioRepositoryAdapter.java` que implementa `UsuarioRepositoryPort` usando
  `UsuarioR2dbcRepository` y el mapper de persistencia
- [ ] T014 Crear `UsuarioPersistenceMapper.java` (MapStruct) para convertir entre `Usuario` y
  `UsuarioEntity`
- [ ] T015 Implementar `GlobalExceptionHandler.java` (`@RestControllerAdvice`) que mapee las excepciones
  de dominio a respuestas HTTP estructuradas:
  - `EmailDuplicadoException` → 409
  - `UsuarioNotFoundException` → 404
  - `CredencialesInvalidasException` → 401
  - `CuentaInactivaException` / `CuentaBanneadaException` → 403
  - `AutoCambioEstadoException` → 409
  - Errores de validación Bean Validation → 400
- [ ] T016 Crear `BeanConfiguration.java` en `infrastructure/config/` para registrar explícitamente todos
  los beans de casos de uso e inyectar los ports correspondientes (sin `@Component` en use cases)
- [ ] T017 Crear `JwtConfig.java` en `infrastructure/config/` para leer `app.jwt.secret` y
  `app.jwt.expiration-ms` desde `application.yml` con `@ConfigurationProperties`. Implementar métodos:
  - `generarToken(Usuario): String` — firma el JWT con el secret e incluye `sub`, `email`, `rol`, `exp`
  - `extraerEmail(String): String` — extrae el claim `email` del token
  - `esValido(String): boolean` — valida firma y expiración
- [ ] T018 Crear `SecurityConfig.java` en `infrastructure/config/` con configuración reactiva
  (`SecurityWebFilterChain`):
  - Rutas públicas: `POST /api/auth/registro`, `POST /api/auth/login`
  - Todas las demás rutas requieren token JWT válido
  - Implementar `JwtAuthenticationWebFilter` que extrae el token del header `Authorization: Bearer <token>`,
    lo valida con `JwtConfig` y puebla el `SecurityContext` con el usuario autenticado
  - Deshabilitar CSRF (API stateless) y sesiones (`SessionCreationPolicy.STATELESS`)
- [ ] T019 Crear script SQL `src/test/resources/schema.sql` para inicializar el esquema en Testcontainers

**Checkpoint**: Tabla creada, dominio modelado, repositorio funcional, seguridad y JWT configurados —
las user stories pueden comenzar.

---

## Phase 3: User Story 1 — Registro de Usuario (Priority: P1)

**Goal**: Cualquier actor puede registrarse con sus datos y un rol válido; el sistema persiste el usuario
con contraseña hasheada y estado `ACTIVO`.

**Independent Test**: `POST /api/auth/registro` con body válido retorna HTTP 201 sin campo `passwordHash`.
`POST /api/auth/registro` con email duplicado retorna HTTP 409.

### Tests para User Story 1

- [ ] T020 [P] [US1] Test de contrato: `POST /api/auth/registro` con datos válidos retorna HTTP 201 con
  usuario en body, sin campo `passwordHash` — `AuthControllerTest.java`
- [ ] T021 [P] [US1] Test de contrato: `POST /api/auth/registro` con email duplicado retorna HTTP 409 —
  `AuthControllerTest.java`
- [ ] T022 [P] [US1] Test de contrato: `POST /api/auth/registro` con campos vacíos retorna HTTP 400 con
  detalle de campos faltantes — `AuthControllerTest.java`
- [ ] T023 [P] [US1] Test de contrato: `POST /api/auth/registro` con rol inválido retorna HTTP 400 —
  `AuthControllerTest.java`
- [ ] T024 [P] [US1] Test unitario de `RegistrarUsuarioUseCase` verificando que la contraseña se hashea
  y que `EmailDuplicadoException` se lanza cuando el email existe — `RegistrarUsuarioUseCaseTest.java`
- [ ] T025 [P] [US1] Test de integración con Testcontainers: flujo POST registro → verificar en PostgreSQL
  que `password_hash` es BCrypt y `estado = 'ACTIVO'` — `UsuarioRepositoryAdapterTest.java`

### Implementación de User Story 1

- [ ] T026 [US1] Implementar `RegistrarUsuarioUseCase.java` en `application/`:
  - Verificar unicidad de email vía `UsuarioRepositoryPort.existePorEmail()` → lanzar `EmailDuplicadoException`
  - Hashear contraseña con `BCryptPasswordEncoder` antes de construir la entidad de dominio
  - Asignar `estado = ACTIVO` y `fechaCreacion = LocalDateTime.now()`
  - Persistir vía `UsuarioRepositoryPort.guardar()` — retornar `Mono<Usuario>`
- [ ] T027 [US1] Crear DTOs `RegistroRequest.java` (con anotaciones `@NotBlank`, `@Email`, `@Size`) y
  `UsuarioResponse.java` (sin campo de contraseña) en `infrastructure/adapter/in/rest/dto/`
- [ ] T028 [US1] Crear `UsuarioRestMapper.java` (MapStruct) para convertir `Usuario` → `UsuarioResponse`
- [ ] T029 [US1] Implementar endpoint `POST /api/auth/registro` en `AuthController.java` retornando
  `Mono<ResponseEntity<UsuarioResponse>>` con HTTP 201 (depende de T026, T027, T028)

**Checkpoint**: Registro de usuarios funcional e independientemente testeable.

---

## Phase 4: User Story 2 — Login de Usuario (Priority: P1)

**Goal**: Un usuario `ACTIVO` puede autenticarse con email y contraseña y recibe un token JWT firmado.
Usuarios `INACTIVO` o `BANNED` reciben HTTP 403.

**Independent Test**: `POST /api/auth/login` con credenciales válidas de usuario activo retorna HTTP 200
con `accessToken`. Mismo request con usuario `BANNED` retorna HTTP 403.

### Tests para User Story 2

- [ ] T030 [P] [US2] Test de contrato: `POST /api/auth/login` con credenciales válidas retorna HTTP 200
  con `accessToken` y `expiresIn` — `AuthControllerTest.java`
- [ ] T031 [P] [US2] Test de contrato: `POST /api/auth/login` con contraseña incorrecta retorna HTTP 401
  con mensaje genérico — `AuthControllerTest.java`
- [ ] T032 [P] [US2] Test de contrato: `POST /api/auth/login` con email inexistente retorna HTTP 401 con
  el mismo mensaje genérico (no revelar existencia del email) — `AuthControllerTest.java`
- [ ] T033 [P] [US2] Test de contrato: `POST /api/auth/login` con usuario `INACTIVO` retorna HTTP 403
  "Cuenta inactiva" — `AuthControllerTest.java`
- [ ] T034 [P] [US2] Test de contrato: `POST /api/auth/login` con usuario `BANNED` retorna HTTP 403
  "Cuenta suspendida" — `AuthControllerTest.java`
- [ ] T035 [P] [US2] Test unitario de `LoginUsuarioUseCase` verificando flujo BCrypt match, verificación
  de estado y generación de token — `LoginUsuarioUseCaseTest.java`
- [ ] T036 [P] [US2] Test de integración con Testcontainers: flujo completo login → token JWT válido
  con claims correctos — `AuthControllerTest.java`

### Implementación de User Story 2

- [ ] T037 [US2] Implementar `LoginUsuarioUseCase.java` en `application/`:
  - Buscar usuario por email vía `UsuarioRepositoryPort.buscarPorEmail()` — lanzar
    `CredencialesInvalidasException` si no existe (sin revelar causa)
  - Verificar contraseña con `BCryptPasswordEncoder.matches()` — lanzar `CredencialesInvalidasException`
    si no coincide
  - Verificar estado: lanzar `CuentaInactivaException` si `INACTIVO`, `CuentaBanneadaException` si `BANNED`
  - Generar token JWT con `JwtConfig.generarToken()` — retornar `Mono<String>` (el token)
- [ ] T038 [US2] Crear DTOs `LoginRequest.java` (email + contraseña con `@NotBlank`) y
  `LoginResponse.java` (con campos `accessToken` y `expiresIn`)
- [ ] T039 [US2] Implementar endpoint `POST /api/auth/login` en `AuthController.java` retornando
  `Mono<ResponseEntity<LoginResponse>>` (depende de T037, T038)

**Checkpoint**: Login funcional con generación de JWT y bloqueo por estado operativos.

---

## Phase 5: User Story 3 — Cambio de Estado de Usuario (Priority: P2)

**Goal**: Un administrador autenticado puede cambiar el estado de cualquier usuario a `ACTIVO`, `INACTIVO`
o `BANNED`; no puede cambiar su propio estado.

**Independent Test**: `PATCH /api/usuarios/{id}/estado` con token JWT válido y body `{ "estado": "BANNED" }`
retorna HTTP 200. El mismo endpoint sin token retorna HTTP 401.

### Tests para User Story 3

- [ ] T040 [P] [US3] Test de contrato: `PATCH /api/usuarios/{id}/estado` con token válido y estado válido
  retorna HTTP 200 con usuario actualizado — `UsuarioControllerTest.java`
- [ ] T041 [P] [US3] Test de contrato: `PATCH /api/usuarios/{id}/estado` sin token retorna HTTP 401 —
  `UsuarioControllerTest.java`
- [ ] T042 [P] [US3] Test de contrato: `PATCH /api/usuarios/{id}/estado` con UUID inexistente retorna
  HTTP 404 — `UsuarioControllerTest.java`
- [ ] T043 [P] [US3] Test de contrato: `PATCH /api/usuarios/{id}/estado` con estado inválido retorna
  HTTP 400 — `UsuarioControllerTest.java`
- [ ] T044 [P] [US3] Test de contrato: administrador intenta cambiar su propio estado retorna HTTP 409
  con mensaje descriptivo — `UsuarioControllerTest.java`
- [ ] T045 [P] [US3] Test unitario de `CambiarEstadoUsuarioUseCase` verificando todos los escenarios de
  error — `CambiarEstadoUsuarioUseCaseTest.java`
- [ ] T046 [P] [US3] Test de integración con Testcontainers: flujo PATCH estado → verificar en PostgreSQL
  → login posterior falla con HTTP 403 si estado es BANNED — `UsuarioControllerTest.java`

### Implementación de User Story 3

- [ ] T047 [US3] Implementar `CambiarEstadoUsuarioUseCase.java` en `application/`:
  - Recibir `idUsuarioObjetivo` (UUID del usuario a cambiar) e `idUsuarioAutenticado` (del SecurityContext)
  - Verificar que no sean el mismo ID → lanzar `AutoCambioEstadoException`
  - Buscar usuario objetivo vía `UsuarioRepositoryPort.buscarPorId()` → lanzar `UsuarioNotFoundException`
    si no existe
  - Actualizar el campo `estado` y persistir vía `UsuarioRepositoryPort.guardar()` — retornar `Mono<Usuario>`
- [ ] T048 [US3] Crear DTOs `CambiarEstadoRequest.java` con campo `estado` (String, `@NotBlank`) con
  validación y conversión a enum `EstadoUsuario`
- [ ] T049 [US3] Implementar endpoint `PATCH /api/usuarios/{id}/estado` en `UsuarioController.java`,
  extrayendo el UUID del usuario autenticado desde el `SecurityContext` reactivo e inyectando
  `CambiarEstadoUsuarioUseCase` — retornar `Mono<ResponseEntity<UsuarioResponse>>`
  (depende de T047, T048)

**Checkpoint**: Las tres user stories son funcionales e independientemente testeables.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Calidad, documentación y verificación de invariantes de seguridad

- [ ] T050 Documentar todos los endpoints con SpringDoc OpenAPI (`@Operation`, `@ApiResponse`,
  `@SecurityRequirement`) y verificar generación correcta del Swagger UI en
  `http://localhost:8081/swagger-ui.html`
- [ ] T051 Agregar `@SecurityScheme` de tipo `HTTP Bearer JWT` en la configuración de OpenAPI para que
  el Swagger UI permita enviar el token en las pruebas interactivas
- [ ] T052 Verificar que ninguna clase dentro de `domain/` importa algo de `org.springframework`,
  `io.r2dbc` o `jakarta.persistence` — el dominio debe ser Java puro
- [ ] T053 Verificar que el campo `passwordHash` nunca aparece en ningún DTO de respuesta (`UsuarioResponse`,
  `LoginResponse`) bajo ninguna circunstancia
- [ ] T054 Revisar mensajes de error para que los endpoints de autenticación no revelen información sobre
  la existencia de emails (respuesta idéntica para email inexistente y contraseña incorrecta)
- [ ] T055 Verificar que activar el perfil `prod` con un `.env` válido arranca el sistema sin errores y que
  ninguna propiedad sensible aparece en los logs de Spring Boot al iniciar
- [ ] T056 Agregar tests unitarios de casos borde en dominio (contraseñas vacías, emails malformados,
  transiciones de estado inválidas)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de Phase 1 — bloquea todas las user stories
- **US1 - Registro (Phase 3)**: Depende de Phase 2
- **US2 - Login (Phase 4)**: Depende de Phase 2 y de US1 (el usuario debe existir para autenticarse)
- **US3 - Cambio de Estado (Phase 5)**: Depende de Phase 2 y de US2 (requiere token JWT para autorizarse)
- **Polish (Phase 6)**: Depende de todas las user stories completadas

### Dentro de cada User Story

- Puerto de salida antes que caso de uso
- Caso de uso antes que controlador y DTOs
- Mappers junto a los DTOs que los usan
- Tests escritos junto a la implementación de cada tarea
- Verificar checkpoint antes de pasar a la siguiente fase

---

## Notes

- El tag `[P]` identifica tareas de prueba; el tag `[US1-US3]` mapea cada tarea a su user story
- **BCrypt**: el hash de la contraseña se realiza en `RegistrarUsuarioUseCase`, no en el controlador ni en
  el repositorio. El dominio recibe `passwordHash` ya calculado; nunca manipula contraseñas en texto plano
- **JWT stateless**: no se almacena el token en base de datos. La invalidación se gestiona por expiración.
  Si se requiere revocación explícita en el futuro, se deberá implementar una blocklist
- **`application.yml` vs `application-prod.yml`**: el perfil dev tiene valores directos seguros para
  versionar (base de datos local, secret de prueba). El perfil prod solo tiene referencias a variables de
  entorno (`${VAR}`). La activación del perfil prod se hace con `SPRING_PROFILES_ACTIVE=prod` en el
  entorno de despliegue
- **`example.env`**: versionado en el repositorio como guía de onboarding. El `.env` real nunca se versiona
- **Gestión de BD**: sin herramienta de migraciones — la tabla se crea manualmente. Para Testcontainers
  se inicializa con `src/test/resources/schema.sql`
- **WebFlux + Spring Security reactivo**: usar `ServerSecurityContextHolder` (no el de servlet) para
  extraer el usuario autenticado en los controladores. El `JwtAuthenticationWebFilter` popula el contexto
  reactivo antes de que llegue al controlador
- **Rol en JWT**: el claim `rol` en el token permite que otros microservicios del ecosistema validen
  permisos sin consultar la base de datos de usuarios en cada request
