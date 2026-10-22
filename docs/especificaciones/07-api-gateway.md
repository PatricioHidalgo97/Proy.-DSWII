# ESP-07 — API Gateway

## 1. Decisión

Se usa **Spring Cloud Gateway Server Web MVC**.

| Aspecto | Decisión |
|---|---|
| Starter | `spring-cloud-starter-gateway-server-webmvc` |
| Runtime | Spring MVC sobre Tomcat |
| Puerto | `${GATEWAY_PORT:9101}` |
| Descubrimiento | Ninguno (URIs fijas por configuración) |
| Seguridad | No valida JWT; delega en los servicios |
| CORS | Centralizado aquí |
| Documentación | Swagger agregado opcional |

## 2. Por qué Gateway MVC y no WebFlux

| Criterio | Gateway MVC | Gateway WebFlux |
|---|---|---|
| Modelo | Bloqueante, Spring MVC | Reactivo, Project Reactor |
| Curva de aprendizaje | Baja para el equipo | Alta |
| Coherencia con el resto | Igual a los servicios actuales | Distinto |
| Rendimiento | Suficiente | Mayor en alta concurrencia |
| Depuración | Sencilla | Requiere entender reactividad |

Para el alcance del proyecto, la simplicidad pesa más que el rendimiento
reactivo.

## 3. Responsabilidades

### 3.1 Lo que hace

- Expone un único punto de entrada HTTP.
- Enruta por prefijo de ruta hacia el servicio correcto.
- Aplica CORS.
- Preserva headers, incluido `Authorization`.
- Expone su propio health check.
- Opcionalmente, agrega los OpenAPI de los servicios.

### 3.2 Lo que no hace

- No valida tokens.
- No aplica reglas de rol.
- No traduce respuestas.
- No agrega lógica de negocio.
- No cachea.
- No reintentar peticiones.

## 4. Rutas

```text
/api/auth/**      -> identity-service
/api/socios/**    -> catalog-service
/api/giros/**     -> catalog-service
/api/puestos/**   -> catalog-service
/api/servicios/** -> catalog-service
/api/bancos/**    -> catalog-service
/api/cuentas/**   -> cash-service
/api/recibos/**   -> cash-service
/api/egresos/**   -> cash-service
/api/reportes/**  -> cash-service
```

### 4.1 Regla de rutas

- No se usa `StripPrefix`: los servicios conservan sus rutas `/api/...`.
- El orden de las rutas es irrelevante porque los prefijos no se solapan.
- Toda ruta no listada devuelve 404 del gateway.

## 5. Configuración de referencia

```yaml
server:
  port: ${GATEWAY_PORT:9101}

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: identity
              uri: ${IDENTITY_URL:http://localhost:9102}
              predicates:
                - Path=/api/auth/**
            - id: catalog
              uri: ${CATALOG_URL:http://localhost:9103}
              predicates:
                - Path=/api/socios/**,/api/giros/**,/api/puestos/**,/api/servicios/**,/api/bancos/**
            - id: cash
              uri: ${CASH_URL:http://localhost:9104}
              predicates:
                - Path=/api/cuentas/**,/api/recibos/**,/api/egresos/**,/api/reportes/**

app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:4400}
```

## 6. CORS

| Parámetro | Valor |
|---|---|
| Orígenes | `http://localhost:4400` |
| Métodos | GET, POST, PUT, DELETE, OPTIONS |
| Headers permitidos | `Authorization`, `Content-Type` |
| Headers expuestos | `Content-Disposition` |
| Credenciales | No |

`Content-Disposition` es necesario para que el navegador lea el nombre del
archivo al descargar los XLSX.

## 7. Actuator

```properties
management.endpoints.web.exposure.include=health,info,gateway
management.endpoint.health.show-details=always
```

| Endpoint | Uso |
|---|---|
| `/actuator/health` | Verificar que el gateway está arriba |
| `/actuator/gateway/routes` | Ver rutas activas |

No se publica Actuator fuera de `localhost`.

## 8. Manejo de errores

| Situación | Respuesta |
|---|---|
| Servicio caído | `503 Service Unavailable` |
| Ruta no existente | `404 Not Found` |
| Timeout | `504 Gateway Timeout` |
| Respuesta del servicio | Se devuelve sin modificar |

El gateway **no transforma** los errores de los servicios: el frontend sigue
recibiendo el JSON `{timestamp, status, message}` original.

## 9. Swagger agregado

### 9.1 Opción recomendada

Swagger por servicio (ver ESP-08). Es más simple y no requiere rutas extra.

### 9.2 Opción agregada

Si se desea un único Swagger:

```yaml
- id: identity-docs
  uri: ${IDENTITY_URL:http://localhost:9102}
  predicates:
    - Path=/docs/identity/**
  filters:
    - RewritePath=/docs/identity/(?<segment>.*), /${segment}
```

```properties
springdoc.swagger-ui.urls[0].name=Identidad
springdoc.swagger-ui.urls[0].url=/docs/identity/v3/api-docs
springdoc.swagger-ui.urls[1].name=Catálogo
springdoc.swagger-ui.urls[1].url=/docs/catalog/v3/api-docs
springdoc.swagger-ui.urls[2].name=Caja
springdoc.swagger-ui.urls[2].url=/docs/cash/v3/api-docs
```

Esta opción se implementa solo si el Swagger por servicio no es suficiente.

## 10. Operación local

```powershell
cd microservices/api-gateway
mvn spring-boot:run
```

Verificación:

```powershell
curl http://localhost:9101/actuator/health
curl http://localhost:9101/api/auth/login -Method POST ...
```

## 11. Checklist del gateway

- [ ] Puerto configurable y sin conflicto.
- [ ] Diez prefijos enrutados correctamente.
- [ ] `Authorization` se propaga intacto.
- [ ] CORS solo en el gateway.
- [ ] 503 cuando un servicio está caído.
- [ ] 404 del gateway para rutas desconocidas.
- [ ] Health check accesible.
- [ ] Sin lógica de negocio.
- [ ] Sin Eureka ni URIs de descubrimiento.
