# Evidencia de bloqueo — PLAN-02 (identity-service)

Evidencia congelada para el planificador de arreglos. No es un resumen: es la
salida literal de la evaluación del orquestador.

## Contexto

- Plan ejecutado: `docs/plans/02-identity-service.md` (PLAN-02).
- Rama: `feature/identity-service`, PR **#7** (OPEN, mergeable, base `main`).
- Base: `6df16478`.
- Entorno: JDK 21 (`C:\Program Files\Java\jdk-21.0.11`), Maven 3.9.16
  (`C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin`),
  MySQL 9.7.1 `root`/`admin`, `JWT_SECRET` compartido y exportado solo en la shell.

## Lo que sí está verificado y correcto

| CA | Resultado | Evidencia literal |
|---|---|---|
| CA-01 | Cumple | `keys: token,id,usuario,nombres,apellidos,rol` |
| CA-02 | Cumple | `payload: {"sub":"admin","usuarioId":1,"nombres":"Administrador","apellidos":"General","rol":"ADMIN","iat":...,"exp":...}` |
| CA-03 | Cumple | `header: {"alg":"HS256"}` / `exp-iat: 28800` |
| CA-04 | Cumple | identity con `--logging.level.org.hibernate.SQL=DEBUG`: tras una llamada **solo** a `/me`, `grep -cE "org.hibernate.SQL\|select .* from usuarios"` → `0` |
| CA-05 | Cumple | token de `POST :9090/api/auth/login` → `GET :9102/api/auth/me` → `HTTP 200` con los 5 campos |
| CA-06 | Cumple | token firmado con `otro-secreto-completamente-distinto-12345` → `401` + `{"status":401,"message":"Token inválido o vencido"}` |
| CA-07 | Cumple | login y `/me` sin campo `password`; los 4 usuarios verificados con `'password' in j → false` |
| CA-08 | Cumple | admin/ADMIN, caja/OPERADOR, caja2/OPERADOR, caja3/OPERADOR, los cuatro `HTTP 200` |
| CA-09 | Cumple (sin navegador) | `/swagger-ui/index.html` → 200; `/v3/api-docs` → 200 sin token |
| CA-10 | Cumple | `paths: ['/api/auth/login','/api/auth/me']`, `securitySchemes: ['bearerAuth']`, `me security: [{"bearerAuth":[]}]`; coincide con el `v3/api-docs` en vivo |
| CA-11 | Cumple | `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| CA-12 | Cumple | `POST :9090/api/auth/login` → `HTTP 200` |
| Postman | Cumple | 5 requests, 17 assertions, **0 fallos** |

Sin secretos versionados: `app.jwt.secret=${JWT_SECRET}` sin valor por defecto
y `git grep` de la clave real no encuentra coincidencias fuera de `docs/`.

## Defecto detectado — el health de identity deja de responder

### Síntoma

```text
$ curl.exe -s -o /dev/null -w "%{http_code}" http://localhost:9102/actuator/health
401

$ curl.exe -s http://localhost:9102/actuator/health
{"status":401,"message":"Debe iniciar sesión"}
```

Antes de PLAN-02 (estado de `main` tras el PR #6) el mismo endpoint devolvía
`{"status":"UP"}`.

### Causa

`microservices/identity-service/src/main/java/com/cibertec/gestion/identity/security/WebSecurityConfig.java`
declara:

```java
.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
.requestMatchers(
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui/index.html"
).permitAll()
.anyRequest().authenticated()
```

`/actuator/health` cae en `anyRequest().authenticated()`.

`docs/plans/02-identity-service.md` §T-05 paso 2 dice literalmente: «Permitir
`POST /api/auth/login`, `OPTIONS /**` y las rutas de Swagger. Exigir
autenticación para el resto.» — el ejecutor cumplió el plan al pie de la letra.

### Criterios que se rompen

| Criterio | Documento | Requisito |
|---|---|---|
| ESP-13 §6 | «Un servicio está terminado cuando […] Su health check responde.» | El health debe responder |
| PLAN-00 CA-03 | `00-fundaciones-y-linea-base.md` §8 | «`/actuator/health` responde `UP` en 9102, 9103 y 9104» |
| PLAN-06 §4 | `06-contrato-y-swagger-agregado.md` | Precondición: «Los tres servicios arrancan — Health de 9102, 9103 y 9104» |

Es una **regresión de un criterio ya cerrado** (PLAN-00 CA-03), no una mejora
opcional.

### Por qué es un defecto de plan y no del ejecutor

El plan no menciona Actuator en T-05, pero PLAN-00 lo exige y ESP-13 §6 lo
eleva a condición de terminado. Cualquier plan posterior (PLAN-03, PLAN-04/05)
va a repetir la misma cadena de seguridad y heredaría el mismo defecto.

## Alcance mínimo esperado del arreglo

1. Permitir `/actuator/health` (público) en la cadena de seguridad de
   `identity-service`.
2. Verificar que sigue devolviendo `{"status":"UP"}` en 9102.
3. Comprobar que `/actuator/health` **no** abre otros endpoints de Actuator
   (`management.endpoints.web.exposure.include=health` ya lo limita).
4. No cambiar nada más del contrato ni de la seguridad de `/api/**`.

## Lo que NO hace falta tocar

- El contrato de login/me: verificado idéntico a ESP-04 §5.1/§5.2.
- JWT, claims, vigencia y validación de secreto: correctos.
- Postman: 17 asserts en verde.
- Flyway: migraciones intactas (`git diff main..HEAD -- database/` vacío).
- `backend/`: intacto.
