# Guía de despliegue: Spring Boot en AWS (consola web)

Guía paso a paso para desplegar cualquier microservicio Spring Boot del ecosistema TicketSeller en AWS usando ECS Fargate + RDS PostgreSQL. Todo desde la consola web de AWS, sin necesidad de CLI.

Aplica a cualquier módulo con el mismo stack (Java 21, WebFlux, R2DBC, PostgreSQL).

---

## Índice

1. [Prerequisitos](#1-prerequisitos)
2. [Arquitectura objetivo](#2-arquitectura-objetivo)
3. [Preparar y subir la imagen Docker](#3-preparar-y-subir-la-imagen-docker)
4. [Crear el repositorio en ECR](#4-crear-el-repositorio-en-ecr)
5. [Crear la base de datos en RDS](#5-crear-la-base-de-datos-en-rds)
6. [Guardar secretos en Secrets Manager](#6-guardar-secretos-en-secrets-manager)
7. [Crear el cluster ECS Fargate](#7-crear-el-cluster-ecs-fargate)
8. [Crear el Task Definition](#8-crear-el-task-definition)
9. [Crear el Application Load Balancer](#9-crear-el-application-load-balancer)
10. [Crear el servicio ECS](#10-crear-el-servicio-ecs)
11. [Inicializar el esquema de base de datos](#11-inicializar-el-esquema-de-base-de-datos)
12. [Configurar CloudWatch Logs](#12-configurar-cloudwatch-logs)
13. [Verificar el despliegue](#13-verificar-el-despliegue)
14. [Variables de entorno requeridas](#14-variables-de-entorno-requeridas)
15. [Despliegue de nuevas versiones](#15-despliegue-de-nuevas-versiones)
16. [Notas para otros módulos](#16-notas-para-otros-módulos)

---

## 1. Prerequisitos

**En tu máquina local:**
- Docker instalado (`docker --version`)
- AWS CLI v2 instalado — solo para el push de la imagen a ECR (no hay forma de subir imágenes Docker desde la consola web)
- `psql` para aplicar el schema SQL a la base de datos

**En AWS:**
- Una cuenta activa
- Usuario IAM con permisos de administrador (o permisos sobre ECR, ECS, RDS, ALB, IAM, Secrets Manager, CloudWatch, VPC)

> La consola web de AWS está en **https://console.aws.amazon.com**. Asegúrate de seleccionar la región correcta en el menú superior derecho (ej. `us-east-1`) y mantenerla igual en todos los pasos.

---

## 2. Arquitectura objetivo

```
Internet
    │
    ▼
Application Load Balancer  (puerto 80 HTTP / 443 HTTPS)
    │
    ▼  health check → /actuator/health
ECS Fargate Task  (puerto 8081)
    │  variables secretas inyectadas desde Secrets Manager
    │
    ▼
RDS PostgreSQL  (puerto 5432, solo accesible desde ECS)
```

Todos los componentes viven en la misma VPC. El RDS nunca es accesible desde internet.

---

## 3. Preparar y subir la imagen Docker

Este paso se hace en tu máquina local. Es el único que requiere terminal.

```bash
# 1. Construir la imagen
cd /ruta/al/proyecto
docker build -t users-module:latest .

# 2. Login a ECR (reemplaza ACCOUNT_ID y REGION con tus valores)
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin \
    123456789012.dkr.ecr.us-east-1.amazonaws.com

# 3. Tag y push (el repositorio ECR se crea en el paso 4)
docker tag users-module:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/users-module:latest

docker push \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/users-module:latest
```

El Account ID lo ves en la consola web: esquina superior derecha → tu nombre de usuario → **Account ID**.

---

## 4. Crear el repositorio en ECR

**Consola AWS → ECR (Elastic Container Registry)**

1. En el panel izquierdo haz clic en **Repositories** → **Create repository**
2. Completa el formulario:
   - **Visibility**: Private
   - **Repository name**: `users-module`
   - Todo lo demás déjalo por defecto
3. Haz clic en **Create repository**
4. Entra al repositorio recién creado y anota la **URI** — se ve así:
   ```
   123456789012.dkr.ecr.us-east-1.amazonaws.com/users-module
   ```
   Esa URI es la que usarás en el paso 3 y en el Task Definition.

Ahora vuelve a tu terminal y ejecuta el push del paso 3 usando esa URI.

---

## 5. Crear la base de datos en RDS

### 5.1 Crear el Security Group para RDS

**Consola AWS → VPC → Security groups → Create security group**

| Campo | Valor |
|---|---|
| Security group name | `sg-rds-users-module` |
| Description | RDS PostgreSQL users-module |
| VPC | selecciona la VPC por defecto |

En **Inbound rules**: no agregues nada todavía.
Haz clic en **Create security group** y anota el **Security group ID** (`sg-xxxxxxxxx`).

### 5.2 Crear la instancia RDS

**Consola AWS → RDS → Create database**

| Sección | Campo | Valor |
|---|---|---|
| Engine | Engine type | PostgreSQL |
| Engine | Version | 16.x (la más reciente) |
| Templates | | **Free tier** (para desarrollo) o Production |
| Settings | DB instance identifier | `users-module-db` |
| Settings | Master username | `ticketseller_admin` |
| Settings | Master password | un password seguro (anótalo) |
| Instance config | DB instance class | `db.t3.micro` |
| Storage | Allocated storage | 20 GiB |
| Storage | Enable storage autoscaling | desmárcalo para Free Tier |
| Connectivity | VPC | la VPC por defecto |
| Connectivity | Public access | **No** |
| Connectivity | VPC security group | **Choose existing** → selecciona `sg-rds-users-module` |
| Additional config | Initial database name | `usuarios_prod` |
| Additional config | Enable automated backups | activado, 7 días |
| Additional config | Enable encryption | activado |

Haz clic en **Create database**. Tarda ~10 minutos. Espera hasta que el estado sea **Available**.

Una vez disponible, entra a la instancia y en la sección **Connectivity & security** anota el **Endpoint** — se verá así:
```
users-module-db.xxxxxxxxx.us-east-1.rds.amazonaws.com
```

La `DB_URL` para configurar la app será:
```
r2dbc:postgresql://users-module-db.xxxxxxxxx.us-east-1.rds.amazonaws.com:5432/usuarios_prod
```

---

## 6. Guardar secretos en Secrets Manager

**Consola AWS → Secrets Manager → Store a new secret**

Repite este proceso **tres veces**, una por cada secreto:

### Secreto 1: JWT Secret

Antes de crear el secreto, genera el valor en tu terminal local:
```bash
openssl rand -base64 64
# Copia el resultado — ese es tu JWT_SECRET
```

En la consola:
1. **Secret type**: Other type of secret
2. **Key/value pairs**: cambia a la pestaña **Plaintext** y pega el valor generado por `openssl`
3. **Secret name**: `/ticketseller/users-module/jwt-secret`
4. Siguiente → Siguiente → **Store**
5. Anota el **ARN** del secreto (lo necesitas en el Task Definition)

### Secreto 2: Password de la base de datos

1. **Secret type**: Other type of secret
2. **Plaintext**: el password que pusiste al crear RDS en el paso 5
3. **Secret name**: `/ticketseller/users-module/db-password`
4. **Store** → anota el ARN

### Secreto 3: URL de la base de datos

1. **Secret type**: Other type of secret
2. **Plaintext**:
   ```
   r2dbc:postgresql://users-module-db.xxxxxxxxx.us-east-1.rds.amazonaws.com:5432/usuarios_prod
   ```
   (usa el endpoint real del paso 5)
3. **Secret name**: `/ticketseller/users-module/db-url`
4. **Store** → anota el ARN

---

## 7. Crear el cluster ECS Fargate

**Consola AWS → ECS → Clusters → Create cluster**

| Campo | Valor |
|---|---|
| Cluster name | `ticketseller` |
| Infrastructure | AWS Fargate (serverless) |

Haz clic en **Create**. En segundos aparece el cluster listo.

---

## 8. Crear el Task Definition

### 8.1 Crear el IAM Role de ejecución

El Task Definition necesita un rol IAM para poder leer Secrets Manager y escribir en CloudWatch.

**Consola AWS → IAM → Roles → Create role**

1. **Trusted entity type**: AWS service
2. **Use case**: busca y selecciona **Elastic Container Service Task**
3. Haz clic en **Next**
4. En **Add permissions**, busca y marca estas dos políticas:
   - `AmazonECSTaskExecutionRolePolicy`
   - `SecretsManagerReadWrite`
5. Haz clic en **Next**
6. **Role name**: `ecsTaskExecutionRole-ticketseller`
7. **Create role**

### 8.2 Crear el Log Group en CloudWatch

**Consola AWS → CloudWatch → Log groups → Create log group**

| Campo | Valor |
|---|---|
| Log group name | `/ecs/users-module` |
| Retention setting | 30 days (o el que prefieras) |

Haz clic en **Create**.

### 8.3 Registrar el Task Definition

**Consola AWS → ECS → Task definitions → Create new task definition**

Selecciona **Create new task definition with JSON** (botón en la esquina superior derecha del formulario) y pega el siguiente JSON reemplazando los valores entre `< >`:

```json
{
  "family": "users-module",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole-ticketseller",
  "containerDefinitions": [
    {
      "name": "users-module",
      "image": "<ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/users-module:latest",
      "portMappings": [
        { "containerPort": 8081, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" },
        { "name": "JWT_EXPIRATION_MS",       "value": "86400000" },
        { "name": "SERVER_PORT",             "value": "8081" },
        { "name": "DB_USERNAME",             "value": "ticketseller_admin" }
      ],
      "secrets": [
        {
          "name": "JWT_SECRET",
          "valueFrom": "<ARN-del-secreto-jwt-secret>"
        },
        {
          "name": "DB_PASSWORD",
          "valueFrom": "<ARN-del-secreto-db-password>"
        },
        {
          "name": "DB_URL",
          "valueFrom": "<ARN-del-secreto-db-url>"
        }
      ],
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -sf http://localhost:8081/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 10,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/users-module",
          "awslogs-region": "<REGION>",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

Haz clic en **Create**. Si el formulario no tiene opción JSON, usa el editor visual:
- **Container name**: `users-module`
- **Image URI**: la URI de ECR del paso 4 con `:latest` al final
- **Port mappings**: Container port `8081`
- En **Environment variables** agrega los 4 valores de texto plano
- En **Secrets** agrega los 3 secretos apuntando a sus ARNs de Secrets Manager
- En **HealthCheck**: `CMD-SHELL`, comando: `curl -sf http://localhost:8081/actuator/health || exit 1`
- En **Logging**: driver `awslogs`, group `/ecs/users-module`

---

## 9. Crear el Application Load Balancer

### 9.1 Crear los Security Groups

Antes de crear el ALB, necesitas tres Security Groups.

**Consola AWS → VPC → Security groups → Create security group**

#### SG 1: para el ALB

| Campo | Valor |
|---|---|
| Name | `sg-alb-users-module` |
| VPC | VPC por defecto |

**Inbound rules** → Add rule:
| Type | Protocol | Port | Source |
|---|---|---|---|
| HTTP | TCP | 80 | `0.0.0.0/0` |
| HTTPS | TCP | 443 | `0.0.0.0/0` |

→ **Create security group** → anota el ID.

#### SG 2: para el ECS Task

| Campo | Valor |
|---|---|
| Name | `sg-ecs-users-module` |
| VPC | VPC por defecto |

**Inbound rules** → Add rule:
| Type | Protocol | Port | Source |
|---|---|---|---|
| Custom TCP | TCP | 8081 | *selecciona el SG del ALB* (`sg-alb-users-module`) |

→ **Create security group** → anota el ID.

#### SG 3: actualizar el de RDS

El `sg-rds-users-module` que creaste en el paso 5 está vacío. Entra a él y agrega:

**Inbound rules → Edit inbound rules → Add rule**:
| Type | Protocol | Port | Source |
|---|---|---|---|
| PostgreSQL | TCP | 5432 | *selecciona el SG del ECS* (`sg-ecs-users-module`) |

→ **Save rules**.

### 9.2 Crear el Target Group

**Consola AWS → EC2 → Target Groups → Create target group**

| Campo | Valor |
|---|---|
| Target type | IP addresses |
| Target group name | `tg-users-module` |
| Protocol | HTTP |
| Port | 8081 |
| VPC | VPC por defecto |
| Health check path | `/actuator/health` |
| Healthy threshold | 2 |
| Unhealthy threshold | 3 |
| Interval | 30 seconds |

Haz clic en **Next** → **Create target group** (no registres targets manualmente, ECS lo hace automáticamente).

### 9.3 Crear el Application Load Balancer

**Consola AWS → EC2 → Load Balancers → Create load balancer → Application Load Balancer**

| Sección | Campo | Valor |
|---|---|---|
| Basic | Load balancer name | `alb-users-module` |
| Basic | Scheme | Internet-facing |
| Basic | IP address type | IPv4 |
| Network | VPC | VPC por defecto |
| Network | Availability Zones | marca **todas** las disponibles |
| Security groups | | remueve el default, agrega `sg-alb-users-module` |
| Listeners | | Protocol HTTP, Port 80 |
| Listeners | Default action | Forward to `tg-users-module` |

Haz clic en **Create load balancer**. Anota el **DNS name** del ALB — lo usarás para probar la app.

#### HTTPS (opcional, requiere dominio propio)

Si tienes un dominio:
1. **Consola AWS → ACM (Certificate Manager) → Request certificate**
2. Ingresa tu dominio (ej. `api.tudominio.com`), método DNS, solicita el certificado y valídalo agregando el registro CNAME en tu DNS.
3. Una vez validado, en el ALB agrega un **Listener HTTPS en puerto 443** apuntando al mismo Target Group.
4. Modifica el Listener HTTP 80 para que redirija a HTTPS: **Action → Redirect → HTTPS 443**.

---

## 10. Crear el servicio ECS

**Consola AWS → ECS → Clusters → ticketseller → Services → Create**

| Sección | Campo | Valor |
|---|---|---|
| Environment | Compute options | Launch type |
| Environment | Launch type | FARGATE |
| Deployment | Task definition | `users-module` (la que creaste) |
| Deployment | Revision | LATEST |
| Deployment | Service name | `users-module` |
| Deployment | Desired tasks | `1` |
| Networking | VPC | VPC por defecto |
| Networking | Subnets | selecciona todas |
| Networking | Security groups | remueve el default, agrega `sg-ecs-users-module` |
| Networking | Public IP | **Turned on** (necesario para que Fargate pueda descargar la imagen de ECR) |
| Load balancing | Load balancer type | Application Load Balancer |
| Load balancing | Load balancer | `alb-users-module` |
| Load balancing | Container | `users-module 8081:8081` |
| Load balancing | Listener | usar el existente en puerto 80 |
| Load balancing | Target group | `tg-users-module` |
| Health check grace period | | `90` segundos |

Haz clic en **Create**. ECS descargará la imagen y levantará el contenedor. Ve a la pestaña **Tasks** del servicio para monitorear el estado. El task pasa de `PROVISIONING` → `PENDING` → `RUNNING` en ~2 minutos.

---

## 11. Inicializar el esquema de base de datos

El proyecto no usa migración automática. El schema se aplica **una sola vez** manualmente.

Como RDS no es público, necesitas conectarte desde dentro de la VPC. La forma más simple es habilitar temporalmente el acceso público a RDS:

1. **Consola AWS → RDS → users-module-db → Modify**
2. En **Connectivity** activa **Publicly accessible → Yes**
3. Haz clic en **Continue → Apply immediately**
4. En `sg-rds-users-module` agrega temporalmente una regla inbound:
   - Type: PostgreSQL, Port: 5432, Source: **My IP**

Desde tu terminal local:
```bash
psql -h users-module-db.xxxxxxxxx.us-east-1.rds.amazonaws.com \
     -U ticketseller_admin \
     -d usuarios_prod \
     -f src/main/resources/db/migration/V1__users.sql
```

Una vez aplicado el schema, vuelve a deshacer los cambios de seguridad:
1. En `sg-rds-users-module` elimina la regla "My IP"
2. En RDS → Modify → **Publicly accessible → No**

---

## 12. Configurar CloudWatch Logs

Los logs del contenedor van automáticamente a CloudWatch gracias al driver configurado en el Task Definition.

**Para ver los logs:**
1. **Consola AWS → CloudWatch → Log groups → /ecs/users-module**
2. Entra al log stream más reciente (formato `ecs/users-module/<task-id>`)
3. Verás la salida completa del contenedor Spring Boot

**Crear una alarma de errores HTTP 5xx (recomendado):**

**Consola AWS → CloudWatch → Alarms → Create alarm**

1. **Select metric** → ALB → Per AppELB, per TG Metrics → busca `HTTPCode_Target_5XX_Count` para `tg-users-module`
2. **Statistics**: Sum, Period: 1 minute
3. **Threshold**: Greater than or equal to `5`
4. **Action**: crear un SNS topic con tu email para recibir notificaciones

---

## 13. Verificar el despliegue

Toma el **DNS name** del ALB (disponible en EC2 → Load Balancers → `alb-users-module`).

```bash
ALB_DNS=alb-users-module-xxxxxxxxx.us-east-1.elb.amazonaws.com

# Health check
curl http://$ALB_DNS/actuator/health
# {"status":"UP"}

# Registro de usuario
curl -X POST http://$ALB_DNS/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test User",
    "email": "test@example.com",
    "telefono": "3001234567",
    "password": "Test1234!"
  }'

# Login
curl -X POST http://$ALB_DNS/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "Test1234!"}'
```

También puedes abrir `http://<ALB_DNS>/swagger-ui.html` en el navegador para usar la UI interactiva.

---

## 14. Variables de entorno requeridas

| Variable | Valor en producción | Cómo configurarla |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Task Definition → Environment variables (texto plano) |
| `DB_URL` | `r2dbc:postgresql://<endpoint>:5432/usuarios_prod` | Secrets Manager → Task Definition → Secrets |
| `DB_USERNAME` | `ticketseller_admin` | Task Definition → Environment variables (texto plano) |
| `DB_PASSWORD` | el password de RDS | Secrets Manager → Task Definition → Secrets |
| `JWT_SECRET` | resultado de `openssl rand -base64 64` | Secrets Manager → Task Definition → Secrets |
| `JWT_EXPIRATION_MS` | `86400000` (24 horas) | Task Definition → Environment variables (texto plano) |
| `SERVER_PORT` | `8081` | Task Definition → Environment variables (texto plano) |

Regla: si el valor es secreto → Secrets Manager. Si es configuración sin riesgo → texto plano en el Task Definition.

---

## 15. Despliegue de nuevas versiones

Cuando cambies código y quieras actualizar la app en producción:

**Desde tu terminal local:**
```bash
# 1. Build y push de la nueva imagen
docker build -t users-module:latest .
docker tag users-module:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/users-module:latest
docker push \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/users-module:latest
```

**Desde la consola web:**
1. **ECS → Clusters → ticketseller → Services → users-module**
2. Haz clic en **Update service**
3. Marca **Force new deployment**
4. Haz clic en **Update**

ECS hace un rolling update automático: levanta el nuevo contenedor, verifica que `/actuator/health` responda `UP`, y solo entonces termina el viejo. Cero downtime.

---

## 16. Notas para otros módulos

Al desplegar `events-module`, `tickets-module`, u otros servicios del ecosistema:

1. **Reutiliza** el cluster `ticketseller` y el role `ecsTaskExecutionRole-ticketseller` — son compartidos.
2. **Crea nuevo** por módulo: repositorio ECR, Task Definition, Target Group, servicio ECS.
3. Cada módulo puede compartir la misma instancia RDS usando databases separadas (`eventos_prod`, `tickets_prod`), o tener instancias RDS independientes.
4. El `JWT_SECRET` **debe ser idéntico** en todos los módulos — los tokens que emite `users-module` son validados por los demás servicios con la misma clave.
5. Los módulos se comunican entre sí usando el DNS del ALB de cada servicio (o ECS Service Discovery si están en la misma VPC).
