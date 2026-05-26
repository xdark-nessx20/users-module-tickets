# Feature Specification: Gestión de Usuarios

**Created**: 26/05/2026

## User Scenarios & Testing *(mandatory)*

---

### User Story 1 - Registro de Usuario (Priority: P1)

Como **cualquier actor del sistema**, quiero poder registrarme con mis datos personales y un rol asignado,
para poder autenticarme y utilizar las funcionalidades que corresponden a mi perfil.

**Why this priority**: Sin usuarios registrados no existe ningún otro flujo del sistema. El registro es
el punto de entrada obligatorio para todos los actores.

**Independent Test**: Un usuario envía una petición de registro con todos los campos obligatorios válidos.
El test es exitoso si el sistema responde con HTTP 201, la contraseña no se devuelve en el body y el
usuario puede iniciar sesión inmediatamente después.

**Acceptance Scenarios**:

1. **Scenario: Registro Exitoso**
    - **Given** que no existe ningún usuario con el email proporcionado.
    - **When** se envía una petición de registro con nombre, email, número de teléfono, contraseña y rol válidos.
    - **Then** el sistema crea el usuario con estado `ACTIVO`, devuelve HTTP 201 con los datos del usuario
      (sin contraseña) y el usuario queda disponible para autenticarse.

2. **Scenario: Email Duplicado**
    - **Given** que ya existe un usuario con el email proporcionado.
    - **When** se intenta registrar otro usuario con ese mismo email.
    - **Then** el sistema devuelve HTTP 409 con el mensaje ***"Ya existe un usuario registrado con ese email"***
      y no crea el registro.

3. **Scenario: Campos Obligatorios Incompletos**
    - **Given** cualquier estado del sistema.
    - **When** se envía una petición de registro con uno o más campos obligatorios vacíos o inválidos.
    - **Then** el sistema devuelve HTTP 400 indicando los campos que no cumplen la validación y no crea
      el registro.

4. **Scenario: Rol Inválido**
    - **Given** cualquier estado del sistema.
    - **When** se envía una petición de registro con un valor de rol que no existe en el enum `RolUsuario`.
    - **Then** el sistema devuelve HTTP 400 con el mensaje ***"Rol no reconocido"*** y no crea el registro.

---

### User Story 2 - Login de Usuario (Priority: P1)

Como **usuario registrado**, quiero poder autenticarme con mi email y contraseña para recibir un token JWT
que me permita acceder a los recursos protegidos del sistema.

**Why this priority**: Sin autenticación no es posible proteger ningún endpoint. Este flujo es prerrequisito
directo de cualquier operación que requiera identidad verificada.

**Independent Test**: Un usuario con estado `ACTIVO` envía credenciales válidas. El test es exitoso si el
sistema devuelve HTTP 200 con un `accessToken` JWT firmado y un campo `expiresIn` indicando el tiempo de
expiración.

**Acceptance Scenarios**:

1. **Scenario: Login Exitoso**
    - **Given** que el usuario existe y su estado es `ACTIVO`.
    - **When** se envían credenciales correctas (email + contraseña).
    - **Then** el sistema devuelve HTTP 200 con un token JWT válido que contiene el `id`, `email` y `rol`
      del usuario en el payload.

2. **Scenario: Credenciales Incorrectas**
    - **Given** que el usuario existe en el sistema.
    - **When** se envía la contraseña incorrecta.
    - **Then** el sistema devuelve HTTP 401 con el mensaje ***"Credenciales inválidas"***. No se debe revelar
      si el email existe o no (mismo mensaje para email inexistente).

3. **Scenario: Usuario INACTIVO o BANNED intenta hacer login**
    - **Given** que el usuario existe pero su estado es `INACTIVO` o `BANNED`.
    - **When** se envían credenciales correctas.
    - **Then** el sistema devuelve HTTP 403 con el mensaje correspondiente:
      ***"Cuenta inactiva"*** o ***"Cuenta suspendida"***.

---

### User Story 3 - Cambio de Estado de Usuario (Priority: P2)

Como **Administrador**, quiero poder cambiar el estado de un usuario entre `ACTIVO`, `INACTIVO` y `BANNED`,
para gestionar el acceso al sistema sin eliminar físicamente los registros.

**Why this priority**: El control de acceso por estado es esencial para la administración del sistema, pero
depende de que el registro y login estén operativos primero.

**Independent Test**: Un administrador envía `PATCH /api/usuarios/{id}/estado` con `{ "estado": "BANNED" }`.
El test es exitoso si la respuesta es HTTP 200, el usuario queda con estado `BANNED` y cualquier intento
de login posterior devuelve HTTP 403.

**Acceptance Scenarios**:

1. **Scenario: Cambio de Estado Exitoso**
    - **Given** que el usuario existe en el sistema.
    - **When** el administrador envía una petición de cambio de estado con un valor válido.
    - **Then** el sistema devuelve HTTP 200 con el usuario actualizado y el nuevo estado persiste.

2. **Scenario: Usuario No Encontrado**
    - **Given** cualquier estado del sistema.
    - **When** se intenta cambiar el estado de un UUID que no corresponde a ningún usuario.
    - **Then** el sistema devuelve HTTP 404 con el mensaje ***"Usuario no encontrado"***.

3. **Scenario: Estado Inválido**
    - **Given** que el usuario existe en el sistema.
    - **When** se envía un valor de estado que no pertenece al enum `EstadoUsuario`.
    - **Then** el sistema devuelve HTTP 400 con el mensaje ***"Estado no reconocido"***.

4. **Scenario: Administrador intenta cambiar su propio estado**
    - **Given** que el administrador autenticado intenta cambiar su propio estado a `INACTIVO` o `BANNED`.
    - **When** se envía la petición con el UUID del propio usuario autenticado.
    - **Then** el sistema devuelve HTTP 409 con el mensaje ***"Un administrador no puede cambiar su propio estado"***.

---

## Edge Cases

- ¿Qué pasa si **se envía un email con formato inválido** durante el registro?  
  El sistema debe rechazarlo con HTTP 400 sin llegar a consultar la base de datos.

- ¿Qué pasa si **el token JWT expira** durante una sesión activa?  
  El sistema devuelve HTTP 401 en el siguiente request protegido. El cliente debe redirigir al login. No se
  implementa refresh token en este alcance.

- ¿Qué pasa si **se registra un número de teléfono con caracteres no numéricos**?  
  El sistema debe validar el formato y rechazar la petición con HTTP 400 indicando el campo `telefono`.

- ¿Qué pasa si **se intenta hacer login con un email que nunca fue registrado**?  
  El sistema devuelve HTTP 401 con el mismo mensaje genérico que para contraseña incorrecta, sin revelar
  si el email existe.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema **DEBE** permitir registrar un usuario con los campos: `nombre`, `email`,
  `telefono`, `contraseña` y `rol`.
- **FR-002**: El sistema **DEBE** validar unicidad del `email` antes de persistir el registro.
- **FR-003**: El sistema **NUNCA DEBE** almacenar contraseñas en texto plano — usar BCrypt.
- **FR-004**: El sistema **DEBE** emitir un token JWT firmado al autenticar exitosamente a un usuario `ACTIVO`.
- **FR-005**: El sistema **DEBE** bloquear el login de usuarios con estado `INACTIVO` o `BANNED`.
- **FR-006**: El sistema **DEBE** permitir cambiar el estado de un usuario a `ACTIVO`, `INACTIVO` o `BANNED`.
- **FR-007**: El sistema **NO DEBE** permitir borrado físico de usuarios, únicamente cambio de estado.
- **FR-008**: El token JWT **DEBE** contener en su payload: `sub` (UUID del usuario), `email`, `rol`
  y `exp` (timestamp de expiración).

### Key Entities *(include if feature involves data)*

1. **Usuario**:
    - Representa a cualquier actor humano del sistema que requiere autenticación.
    - **Atributos**: `id` (UUID, generado), `nombre` (String), `email` (String, único), `telefono` (String),
      `passwordHash` (String), `rol` (enum `RolUsuario`), `estado` (enum `EstadoUsuario`),
      `fechaCreacion` (LocalDateTime)

2. **RolUsuario** *(enum)*:
    - `ADMINISTRADOR_RECINTO`
    - `GESTOR_INVENTARIO`
    - `COORDINADOR_PATROCINIOS`
    - `AGENTE_VENTAS`
    - `COMPRADOR`
    - `PROMOTOR_EVENTOS`
    - `CONTROLADOR_ACCESOS`
    - `ADMINISTRADOR_FINANCIERO`

3. **EstadoUsuario** *(enum)*:
    - `ACTIVO` — puede autenticarse y operar normalmente.
    - `INACTIVO` — no puede autenticarse; el registro se conserva.
    - `BANNED` — acceso suspendido por razón administrativa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El registro de un nuevo usuario debe completarse en una respuesta inferior a 500ms.
- **SC-002**: El login exitoso debe devolver un token JWT en menos de 300ms.
- **SC-003**: No debe existir ningún endpoint protegido accesible sin un token JWT válido y activo.
- **SC-004**: Las contraseñas almacenadas deben ser hashes BCrypt — nunca texto plano. Verificable
  directamente en la base de datos.
- **SC-005**: Un usuario con estado `BANNED` o `INACTIVO` no debe poder obtener un token JWT bajo
  ninguna circunstancia.
