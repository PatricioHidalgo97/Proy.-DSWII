# ESP-08 — OpenAPI y Swagger

## 1. Decisión

Cada servicio expone su propia documentación con **springdoc-openapi**.

| Aspecto | Decisión |
|---|---|
| Librería | `springdoc-openapi-starter-webmvc-ui` |
| Versión | `2.8.14` |
| Formato | OpenAPI 3 |
| UI por servicio | Sí |
| UI agregada | Opcional (ver ESP-07) |
| Seguridad | Bearer JWT documentado |

## 2. Dependencia

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.8.14</version>
</dependency>
```

`springdoc 2.8.x` es la línea compatible con Spring Boot 3.5.x.

## 3. Configuración

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
```

## 4. Esquema de seguridad

En cada servicio, una clase de configuración declara el esquema Bearer:

```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API de catálogo",
        version = "1.0",
        description = "Socios, giros, puestos, servicios y bancos"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {}
```

Luego, a nivel de controlador o de operación:

```java
@SecurityRequirement(name = "bearerAuth")
```

El botón **Authorize** de Swagger UI permite pegar el token obtenido en
`/api/auth/login`.

## 5. Permisos en la cadena de seguridad

```java
.requestMatchers(
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
).permitAll()
```

Sin esta regla, Swagger devuelve 401.

## 6. URLs

| Servicio | Swagger UI | OpenAPI JSON |
|---|---|---|
| identity-service | `http://localhost:9102/swagger-ui.html` | `http://localhost:9102/v3/api-docs` |
| catalog-service | `http://localhost:9103/swagger-ui.html` | `http://localhost:9103/v3/api-docs` |
| cash-service | `http://localhost:9104/swagger-ui.html` | `http://localhost:9104/v3/api-docs` |
| api-gateway | `http://localhost:9101/swagger-ui.html` | `http://localhost:9101/v3/api-docs` (si se agrega) |

## 7. Contenido mínimo esperado

### 7.1 identity-service

- Tag `Autenticación`
- `POST /api/auth/login` con ejemplo de request y response
- `GET /api/auth/me`
- Respuestas 200, 401

### 7.2 catalog-service

- Un tag por recurso: Socios, Giros, Puestos, Servicios, Bancos
- Operaciones GET, POST, PUT, DELETE
- Ejemplos de payload para cada request
- Respuestas 200, 201, 204, 400, 401, 403, 404, 409

### 7.3 cash-service

- Tags: Cuentas, Recibos, Egresos, Reportes
- Documentar los endpoints de lote
- Documentar el endpoint multipart de importación CSV
- Documentar `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  en los reportes
- Ejemplos de cuenta por puesto y por socio

## 8. Exportación del contrato

Se guarda una copia versionada del contrato de cada servicio:

```text
docs/openapi/
├─ identity.json
├─ catalog.json
└─ cash.json
```

### 8.1 Cómo exportarlo

```powershell
curl http://localhost:9102/v3/api-docs -o docs/openapi/identity.json
curl http://localhost:9103/v3/api-docs -o docs/openapi/catalog.json
curl http://localhost:9104/v3/api-docs -o docs/openapi/cash.json
```

### 8.2 Cuándo actualizarlo

Cada vez que cambie un endpoint, un DTO o un código de respuesta. El archivo
exportado es evidencia de que el contrato público no se rompió.

## 9. Uso de anotaciones

| Anotación | Dónde | Para qué |
|---|---|---|
| `@Tag` | Controlador | Agrupar operaciones |
| `@Operation` | Método | Resumen y descripción |
| `@ApiResponse` | Método | Códigos y ejemplos |
| `@Parameter` | Parámetro | Descripción y ejemplo |
| `@Schema` | DTO | Descripción y ejemplo de campo |
| `@SecurityRequirement` | Clase o método | Requiere Bearer |

## 10. Relación con Postman

| Artefacto | Propósito |
|---|---|
| Swagger UI | Explorar y probar endpoints individuales |
| OpenAPI JSON | Contrato versionado |
| Colección Postman | Flujo completo con asserts automatizables |

Los tres se complementan; ninguno reemplaza a otro.

## 11. Checklist de documentación

- [ ] springdoc en los tres servicios.
- [ ] Swagger UI accesible sin token.
- [ ] Botón Authorize funcional con JWT.
- [ ] Todos los endpoints documentados.
- [ ] Códigos de error documentados.
- [ ] Ejemplos de request y response.
- [ ] Reportes documentados como descarga binaria.
- [ ] OpenAPI exportado a `docs/openapi/`.
