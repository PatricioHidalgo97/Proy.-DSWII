# ESP-05 — Seguridad y JWT

## 1. Decisión

Se conserva el modelo actual: **JWT firmado con HMAC-SHA256, sin sesiones en
servidor, con BCrypt para contraseñas**.

Cambia una sola cosa: la validación del token se replica en los tres servicios,
usando el mismo secreto.

## 2. Emisión

Solo **identity-service** emite tokens.

### 2.1 Endpoint

```http
POST /api/auth/login
Content-Type: application/json

{ "usuario": "admin", "password": "Admin123*" }
```

### 2.2 Claims

| Claim | Tipo | Origen |
|---|---|---|
| `sub` | string | `usuario` |
| `usuarioId` | number | `id` del usuario |
| `nombres` | string | nombres |
| `apellidos` | string | apellidos |
| `rol` | string | `ADMIN` o `OPERADOR` |
| `iat` | number | fecha de emisión |
| `exp` | number | emisión + expiración |

### 2.3 Algoritmo y vigencia

| Parámetro | Valor |
|---|---|
| Algoritmo | HS256 |
| Secreto | `${JWT_SECRET}` compartido |
| Vigencia por defecto | 8 horas (28 800 000 ms) |
| Renovación | No hay refresh token; se vuelve a iniciar sesión |

## 3. Validación

Cada servicio implementa su propio filtro JWT, equivalente al actual
`JwtAuthenticationFilter`:

1. Lee el header `Authorization: Bearer <token>`.
2. Si no existe, continúa sin autenticación y la cadena de seguridad decide.
3. Si existe, valida firma y expiración.
4. Construye un `Authentication` con autoridad `ROLE_<rol>`.
5. Si el token es inválido, responde **401** con:

```json
{ "status": 401, "message": "Token inválido o vencido" }
```

## 4. Principal enriquecido

**Decisión:** el `Principal` deja de ser solo el nombre de usuario y pasa a ser
un objeto con los datos del token.

```java
public record UsuarioPrincipal(
    Long id,
    String usuario,
    String nombres,
    String apellidos,
    String rol
) {}
```

Motivo: caja necesita el nombre y el rol para guardar el snapshot del usuario en
recibos y egresos, sin llamar a identidad.

Consecuencia: donde hoy se usa `Authentication.getName()`, se debe leer
`UsuarioPrincipal`. El contrato HTTP no cambia.

## 5. Reglas de autorización

Se conservan exactamente las reglas actuales:

| Método | Ruta | Regla |
|---|---|---|
| POST | `/api/auth/login` | Público |
| OPTIONS | `/**` | Público |
| DELETE | `/api/**` | `ADMIN` |
| POST | `/api/socios`, `/api/giros`, `/api/puestos`, `/api/servicios`, `/api/bancos` | `ADMIN` |
| PUT | `/api/socios/**`, `/api/giros/**`, `/api/puestos/**`, `/api/servicios/**`, `/api/bancos/**` | `ADMIN` |
| PUT | `/api/egresos/*/anular` | `ADMIN` |
| GET | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Público |
| Resto | — | Autenticado |

La regla de `ADMIN` para escrituras de catálogo se aplica **en
catalog-service**, no en el gateway. El gateway no decide permisos.

## 6. Contraseñas

- Se guardan con BCrypt, factor 10.
- Nunca se devuelven en ninguna respuesta.
- El campo `password` se excluye de la serialización.
- Los usuarios iniciales se cargan desde migraciones Flyway con hashes ya
  calculados.

### 6.1 Usuarios iniciales

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123*` | ADMIN |
| `caja` | `Caja123*` | OPERADOR |
| `caja2` | `Demo123*` | OPERADOR |
| `caja3` | `Demo123*` | OPERADOR |

> Estas credenciales son de demostración académica. En un entorno real se
> definen por variables de entorno y no se versionan.

## 7. Secreto compartido

**Regla:** `JWT_SECRET` debe ser idéntico en `identity-service`,
`catalog-service` y `cash-service`.

| Situación | Consecuencia |
|---|---|
| Secreto distinto | Los servicios rechazan tokens válidos con 401 |
| Secreto rotado solo en identidad | Todos los tokens nuevos fallan en los demás |
| Secreto vacío | La aplicación no debe arrancar |

Recomendación: validar al arranque que el secreto tenga al menos 32 caracteres.

## 8. CORS

### 8.1 Decisión

CORS se configura **solo en el gateway**. Los servicios de dominio no reciben
tráfico directo del navegador.

| Origen permitido | Uso |
|---|---|
| `http://localhost:4400` | Frontend Angular de este proyecto |

### 8.2 Configuración

```yaml
app:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:4400}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: Authorization,Content-Type
    exposed-headers: Content-Disposition
```

`Content-Disposition` se expone porque los reportes XLSX lo usan para el nombre
del archivo.

### 8.3 Excepción para Swagger

Cada servicio permite el acceso público a su propia documentación:

```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
.permitAll()
```

Esto no abre CORS: solo permite abrir el Swagger directamente en el navegador.

## 9. Propagación en llamadas internas

Cuando caja llama a catálogo, reenvía el token original del usuario:

```http
Authorization: Bearer <token>
```

No se crean tokens técnicos ni se usa autenticación básica entre servicios.

### 9.1 Motivo

- Mantiene una sola fuente de autorización.
- Evita credenciales adicionales que rotar.
- Permite auditar la operación como la acción del usuario.

### 9.2 Limitación aceptada

Si en el futuro catálogo necesita endpoints que el usuario no puede invocar, se
resolverá con un scope específico. No es necesario hoy.

## 10. Cierre de sesión

Se mantiene del lado del cliente:

1. El frontend elimina el token del almacenamiento local.
2. El interceptor deja de enviar el header.
3. El gateway no mantiene estado.

No hay blacklist de tokens. Un token robado sigue siendo válido hasta su
expiración. Se acepta por el alcance académico.

## 11. Checklist de seguridad

- [ ] `JWT_SECRET` igual en los tres servicios.
- [ ] `password` nunca serializado.
- [ ] BCrypt con factor 10.
- [ ] 401 con mensaje uniforme para token ausente o inválido.
- [ ] 403 con mensaje uniforme para rol insuficiente.
- [ ] CORS solo en el gateway.
- [ ] Swagger público solo para documentación.
- [ ] Escrituras de catálogo restringidas a `ADMIN`.
- [ ] Anulación de egresos restringida a `ADMIN`.
- [ ] Sin credenciales en el repositorio.
