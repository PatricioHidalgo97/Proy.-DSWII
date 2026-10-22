# PLAN-02 — identity-service

## 1. Objetivo

Extraer la identidad del monolito a un servicio independiente sobre
`gestion_auth`, que emita JWT con el mismo algoritmo, claims y vigencia, y que
exponga `POST /api/auth/login` y `GET /api/auth/me` a través del gateway sin
cambiar el contrato.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-01 | §1, §3.1, §4 | Rol del servicio y dirección de dependencias |
| ESP-03 | §1, §2 | Dueño único de `usuarios`; nadie más lo lee |
| ESP-04 | §4.1, §5.1, §5.2, §6 | Contrato de identidad y formato de errores |
| ESP-05 | §1, §2, §3, §4, §5, §6, §7, §10, §11 | JWT, claims, `UsuarioPrincipal`, autorización, BCrypt |
| ESP-06 | §1, §2, §3 | `gestion_auth`, Flyway `auth`, `ddl-auto=validate` |
| ESP-08 | §2, §3, §4, §5, §6, §7.1, §8 | springdoc y exportación |
| ESP-11 | §3 (F1), §4.1, §7 (Fase 1), §8 | Qué se mueve y criterios de salida |
| ESP-12 | R-01, R-04, R-13 | Riesgos aplicables |
| ESP-13 | §1.2, §3, §5, §5.1, §6 | Nomenclatura, estilo, pruebas, terminado |

## 3. Alcance

### Dentro

- Entidad `UsuarioEntity`, enum `Rol`, repositorio y servicio de usuario.
- `JwtService` con los cinco claims de ESP-05 §2.2.
- `UsuarioPrincipal` con los cinco campos de ESP-05 §4.
- Filtro JWT y cadena de seguridad propios del servicio.
- `AuthController` con login y me.
- BCrypt factor 10 y `password` no serializado.
- Manejo de errores con la forma de ESP-04 §6.
- springdoc con esquema Bearer y exportación de `docs/openapi/identity.json`.
- Migración Flyway de `gestion_auth` aplicada desde el servicio.
- Pruebas equivalentes a `AuthIntegrationTest`.
- Carpeta `01 · Autenticación` completa en Postman.

### Fuera

- Emisión de tokens para otros servicios (cada uno valida, no emite).
- Refresh token, blacklist o cierre de sesión del lado servidor.
- CRUD de usuarios.
- Cambios en `database/flyway/auth/**`.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-00 cerrado | Índice de `docs/plans/README.md` |
| PLAN-01 cerrado | El gateway enruta `/api/auth/**` |
| `gestion_auth` existe y su Flyway aplica | `flyway_schema_history` con `V1` y `V2` |
| `JWT_SECRET` definido en el entorno con al menos 32 caracteres | `$env:JWT_SECRET.Length` |
| `docs/contract/baseline-contrato.md` disponible | Archivo del PLAN-00 |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-06 compara el JSON con la línea base campo por campo; T-09 lo verifica con asserts |
| R-04 Secretos JWT distintos | Alto | T-02 valida el secreto al arranque y falla si mide menos de 32 caracteres; CA-05 comprueba que un token del monolito se acepta |
| R-13 Pérdida del respaldo | Alto | T-03 y T-04 copian clases de `backend/`, no las mueven: el monolito queda intacto |

## 6. Tareas

### T-01 — Crear la capa de persistencia de identidad

- **Objetivo:** mapear `usuarios` con `ddl-auto=validate`.
- **Especificación:** ESP-06 §1, §3; ESP-13 §1.2.
- **Archivos:**
  - `.../identity/entities/UsuarioEntity.java`
  - `.../identity/enums/Rol.java`
  - `.../identity/repositories/UsuarioRepository.java`
- **Depende de:** PLAN-00.
- **Pasos:**
  1. Copiar `UsuarioEntity` de `backend/src/main/java/com/cibertec/gestion/entities/UsuarioEntity.java` cambiando solo el paquete.
  2. Conservar `@JsonIgnore` en `password`.
  3. Copiar `Rol` y `UsuarioRepository` sin cambios de lógica.
  4. No agregar campos ni relaciones.
- **Verificación:** el servicio arranca y `ddl-auto=validate` no falla contra `gestion_auth`.
- **Terminado cuando:** Hibernate valida el esquema y `findByUsuario` funciona.

### T-02 — Implementar JWT y `UsuarioPrincipal`

- **Objetivo:** emitir y validar tokens con los claims de ESP-05.
- **Especificación:** ESP-05 §2.2, §2.3, §4, §7.
- **Archivos:**
  - `.../identity/security/JwtService.java`
  - `.../identity/security/UsuarioPrincipal.java`
  - `.../identity/security/JwtAuthenticationFilter.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar `JwtService` del monolito; conservar HS256, los cinco claims y `expiration-ms`.
  2. Crear `UsuarioPrincipal` como `record` con `id`, `usuario`, `nombres`, `apellidos`, `rol`.
  3. En el filtro, construir el `Authentication` con `UsuarioPrincipal` como principal y `ROLE_<rol>` como autoridad.
  4. Mantener el 401 con el cuerpo exacto de ESP-05 §3.
  5. Validar al arranque que el secreto tenga al menos 32 caracteres; si no, abortar el arranque.
- **Verificación:** un token del monolito se acepta en identidad y uno firmado con otro secreto devuelve 401.
- **Terminado cuando:** ambos casos se comportan como describe ESP-05 §7.

### T-03 — Implementar el login

- **Objetivo:** autenticar y devolver la forma exacta de ESP-04 §5.1.
- **Especificación:** ESP-04 §5.1; ESP-05 §6.
- **Archivos:**
  - `.../identity/services/IUsuarioService.java`
  - `.../identity/services/UsuarioService.java`
  - `.../identity/dto/LoginRequest.java`
  - `.../identity/dto/LoginResponse.java`
- **Depende de:** T-02.
- **Pasos:**
  1. Copiar `UsuarioService.login` sin cambiar las reglas: usuario inexistente, inactivo o password incorrecta devuelven el mismo 401.
  2. Declarar `LoginResponse` con `token`, `id`, `usuario`, `nombres`, `apellidos`, `rol`, en ese orden.
  3. Aplicar Bean Validation en `LoginRequest`.
  4. No devolver `password` en ninguna ruta.
- **Verificación:** `POST /api/auth/login` con `admin`/`Admin123*` devuelve 200 y los seis campos.
- **Terminado cuando:** el JSON coincide con la línea base.

### T-04 — Implementar `GET /api/auth/me`

- **Objetivo:** devolver el usuario del token, sin consultar la base.
- **Especificación:** ESP-04 §5.2; ESP-05 §4.
- **Archivos:**
  - `.../identity/dto/UsuarioResponse.java`
  - `.../identity/controllers/AuthController.java`
- **Depende de:** T-03.
- **Pasos:**
  1. Crear `UsuarioResponse` con `id`, `usuario`, `nombres`, `apellidos`, `rol`.
  2. Implementar `me` leyendo el `UsuarioPrincipal` del contexto de seguridad.
  3. No consultar `UsuarioRepository` en este endpoint.
- **Verificación:** el endpoint responde 200 con los cinco campos y no ejecuta SQL.
- **Terminado cuando:** la respuesta coincide con ESP-04 §5.2.

### T-05 — Configurar la cadena de seguridad

- **Objetivo:** replicar las reglas de ESP-05 §5 sin CORS.
- **Especificación:** ESP-05 §5, §8.3, §11.
- **Archivos:** `.../identity/security/WebSecurityConfig.java`
- **Depende de:** T-02.
- **Pasos:**
  1. Copiar la cadena del monolito y **eliminar** la configuración de CORS.
  2. Permitir `POST /api/auth/login`, `OPTIONS /**`, las rutas de Swagger y
     `/actuator/health` (ESP-13 §6).
  3. Exigir autenticación para el resto.
  4. Mantener los cuerpos 401 y 403 de ESP-05.
  5. No incluir reglas de catálogo ni de egresos: no existen en este servicio.
- **Verificación:** `GET /api/auth/me` sin token devuelve 401 y con token devuelve 200.
- **Terminado cuando:** las reglas aplicables a identidad están activas y no hay CORS.

### T-06 — Alinear el contrato con la línea base

- **Objetivo:** garantizar que el JSON no cambió.
- **Especificación:** ESP-04 §1, §7, §8; ESP-13 §4.
- **Archivos:** ninguno.
- **Depende de:** T-03, T-04.
- **Pasos:**
  1. Comparar la respuesta de login con ESP-04 §5.1 campo por campo.
  2. Comparar la respuesta de me con ESP-04 §5.2.
  3. Comparar el error de credenciales con ESP-04 §6.
  4. Anotar cualquier diferencia y detener el plan si la hay.
- **Verificación:** las tres comparaciones coinciden exactamente.
- **Terminado cuando:** no hay diferencias.

### T-07 — Agregar springdoc y exportar el contrato

- **Objetivo:** documentar y exportar el contrato de identidad.
- **Especificación:** ESP-08 §2, §3, §4, §5, §6, §7.1, §8.
- **Archivos:**
  - `microservices/identity-service/pom.xml` (dependencia springdoc 2.8.14)
  - `.../identity/config/OpenApiConfig.java`
  - `docs/openapi/identity.json`
- **Depende de:** T-05.
- **Pasos:**
  1. Agregar `springdoc-openapi-starter-webmvc-ui` 2.8.14.
  2. Configurar `@OpenAPIDefinition` y `@SecurityScheme(bearerAuth)`.
  3. Anotar el controlador con `@Tag("Autenticación")` y los dos métodos con `@Operation`.
  4. Documentar respuestas 200 y 401.
  5. Exportar: `curl http://localhost:9102/v3/api-docs -o docs/openapi/identity.json`.
- **Verificación:** Swagger UI abre en 9102 sin token, el botón Authorize funciona y el JSON exportado contiene los dos paths.
- **Terminado cuando:** el archivo exportado existe y documenta los dos endpoints.

### T-08 — Portar las pruebas de autenticación

- **Objetivo:** conservar al menos una prueba por regla migrada.
- **Especificación:** ESP-13 §5, §5.1.
- **Archivos:** `microservices/identity-service/src/test/java/com/cibertec/gestion/identity/security/AuthIntegrationTest.java`
- **Depende de:** T-05.
- **Pasos:**
  1. Copiar `backend/src/test/java/com/cibertec/gestion/security/AuthIntegrationTest.java`.
  2. Adaptar el paquete y el usuario de prueba a un usuario sembrado por Flyway.
  3. Mantener los tres casos: login correcto, login incorrecto, ruta protegida sin token.
  4. Agregar un caso: token firmado con otro secreto devuelve 401.
- **Verificación:** `mvn test` en `identity-service` pasa los cuatro casos.
- **Terminado cuando:** las cuatro pruebas pasan.

### T-09 — Completar la carpeta de autenticación en Postman

- **Objetivo:** cubrir los cuatro casos de ESP-09 §4.1.
- **Especificación:** ESP-09 §3, §4.1, §7.1.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-03, T-04.
- **Pasos:**
  1. Agregar login de operador con `caja`/`Caja123*` y guardar `tokenOperador`.
  2. Agregar `GET /auth/me` con assert de `usuario` y `rol`.
  3. Agregar login inválido con assert 401.
  4. Verificar que el assert del login admin sigue guardando `token`.
- **Verificación:** `npx newman run` ejecuta la carpeta en verde.
- **Terminado cuando:** los cuatro ítems pasan por el gateway.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Persistencia |
| 2 | T-02, T-05 | JWT y cadena de seguridad son independientes de los endpoints |
| 3 | T-03 | Login, requiere T-02 |
| 4 | T-04, T-08 | Me requiere T-03; las pruebas requieren T-05 |
| 5 | T-06, T-07, T-09 | Contrato, Swagger y Postman requieren los dos endpoints |

T-06 debe ejecutarse antes de exportar el contrato en T-07: si hay diferencias,
el plan se detiene.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | El login devuelve los seis campos del contrato | Comparación con ESP-04 §5.1 | ESP-04 §5.1 |
| CA-02 | El token contiene los cinco claims | Decodificar el payload del JWT | ESP-05 §2.2 |
| CA-03 | El algoritmo es HS256 y la vigencia 8 horas | Cabecera y `exp - iat` | ESP-05 §2.3 |
| CA-04 | `/api/auth/me` no consulta la base | Log SQL sin `select` de `usuarios` | ESP-05 §4 |
| CA-05 | Un token del monolito es aceptado | Llamada cruzada con el mismo secreto | ESP-05 §7 |
| CA-06 | Un secreto distinto produce 401 | Token firmado con otra clave | ESP-05 §7 |
| CA-07 | `password` nunca se serializa | Respuesta de login y de me sin el campo | ESP-05 §6 |
| CA-08 | Las credenciales de los cuatro usuarios funcionan | Cuatro logins correctos | ESP-05 §6.1 |
| CA-09 | Swagger UI abre sin token y Authorize funciona | Navegador en 9102 | ESP-08 §5 |
| CA-10 | El contrato está exportado | `docs/openapi/identity.json` con dos paths | ESP-08 §8 |
| CA-11 | Las pruebas pasan | `mvn test` en verde | ESP-13 §5.1 |
| CA-12 | El monolito sigue arrancando | Login directo en 9090 | ESP-11 §9 |
| CA-13 | El health responde sin token y no abre el resto de Actuator | `GET /actuator/health` → 200 `{"status":"UP"}` y `GET /actuator/env` sin token → 401 | ESP-13 §6 |

## 9. Verificación

```powershell
# Arranque
# JWT_SECRET se toma del entorno local; no se escribe en el plan (contrato de planes, seccion 4.2)
if (-not $env:JWT_SECRET) { throw 'Defina JWT_SECRET antes de arrancar los servicios' }
cd microservices/identity-service
mvn spring-boot:run

# CA-01 / CA-02 / CA-03
$login = curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json
$login | Format-List
$payload = $login.token.Split('.')[1]
$payload.PadRight(($payload.Length + 3) - ($payload.Length % 4), '=') `
  -replace '-','+' -replace '_','/' | ForEach-Object {
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) | ConvertFrom-Json
  }

# CA-05
curl.exe -s -X POST http://localhost:9090/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json |
  ForEach-Object { curl.exe -s http://localhost:9102/api/auth/me -H "Authorization: Bearer $($_.token)" }

# CA-07
curl.exe -s http://localhost:9102/api/auth/me -H "Authorization: Bearer $($login.token)"

# CA-10
curl.exe http://localhost:9102/v3/api-docs -o docs/openapi/identity.json

# CA-11
mvn test

# CA-12
cd ../../backend ; mvn spring-boot:run

# CA-13 (health publico y resto de Actuator cerrado)
# GET /actuator/health debe responder {"status":"UP"} sin token
curl.exe -s http://localhost:9102/actuator/health
# GET /actuator/env sin token debe responder 401
curl.exe -s -o NUL -w "%{http_code}" http://localhost:9102/actuator/env
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 a T-05 | Detener el servicio; el gateway vuelve a enrutar `/api/auth/**` al monolito |
| T-07 | Quitar la dependencia springdoc y borrar `docs/openapi/identity.json` |
| Cualquier punto | `DROP DATABASE gestion_auth` y reaplicar; el monolito conserva su propia base |

## 11. Entregables

```text
microservices/identity-service/src/main/java/com/cibertec/gestion/identity/
├─ IdentityServiceApplication.java
├─ config/OpenApiConfig.java
├─ controllers/AuthController.java
├─ dto/{LoginRequest,LoginResponse,UsuarioResponse}.java
├─ entities/UsuarioEntity.java
├─ enums/Rol.java
├─ exceptions/{ApiExceptionHandler,BusinessException,NotFoundException}.java
├─ repositories/UsuarioRepository.java
├─ security/{JwtService,JwtAuthenticationFilter,WebSecurityConfig,UsuarioPrincipal}.java
└─ services/{IUsuarioService,UsuarioService}.java
microservices/identity-service/src/test/java/.../AuthIntegrationTest.java
docs/openapi/identity.json
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json (carpeta 01)
```

## 12. Fuera de alcance

- Registro de usuarios, cambio de contraseña o recuperación.
- CRUD de usuarios o de roles.
- Refresh token y blacklist.
- CORS en este servicio.
- Emisión de tokens desde cualquier otro servicio.
- Modificación de `database/flyway/auth/**` o de `backend/`.
