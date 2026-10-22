# PLAN-08 — Arreglo del health de identity y alineación de la cadena de seguridad

## 1. Objetivo

Dejar `identity-service` con `/actuator/health` público devolviendo
`{"status":"UP"}` en su puerto, sin abrir el resto de Actuator ni relajar
`/api/**`, y corregir los planes de catálogo y caja para que la misma regla
exista antes de que se ejecuten.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-13 | §5.1, §6 | El health check es condición de terminado de un servicio; las pruebas conservan cobertura |
| ESP-05 | §3, §5, §8.1, §11 | Solo se añade una ruta pública de observabilidad; `/api/**` sigue autenticado; sin CORS en el servicio |
| ESP-04 | §1, §5.1, §5.2, §6 | El contrato de login, de `me` y el cuerpo de error 401 no cambian |
| ESP-02 | §1, §3 | Puerto de identidad y variables de entorno; no se fijan valores nuevos en código |
| ESP-08 | §5, §8 | Swagger sigue público y `docs/openapi/identity.json` no cambia |
| ESP-09 | §3, §6, §8 | La colección sigue siendo el contrato ejecutable y conserva sus peticiones |
| ESP-11 | §7 (Fase 1), §10 | Criterios de salida de la fase y conservación del monolito |
| ESP-07 | §7 | El health del gateway no se altera |
| ESP-12 | R-01, R-04, R-06, R-12, R-13 | Riesgos aplicables y su mitigación en este plan |

> Nota de interpretación: ESP-05 §5 enumera las rutas de la API y de Swagger y
> cierra con «Resto — Autenticado». `/actuator/health` no aparece en esa tabla,
> mientras que ESP-13 §6 y el criterio ya cerrado PLAN-00 CA-03 exigen que el
> health responda `UP`, y ese era el comportamiento verificado en 9102, 9103 y
> 9104 antes de PLAN-02. No se trata como contradicción de especificaciones: la
> regla específica de salud (ESP-13 §6) prevalece sobre el cierre genérico de
> ESP-05 §5, y el arreglo restaura un comportamiento que ya existía. Añadir la
> fila `GET /actuator/health | Público` a ESP-05 §5 queda como decisión humana
> (§12) y no bloquea la ejecución: lo que falta es la fila en la tabla, no la
> regla.

## 3. Alcance

### Dentro

- Una regla `permitAll` para `/actuator/health` en la cadena de seguridad de
  `identity-service`.
- Dos pruebas de regresión: health público sin detalles y resto de Actuator
  cerrado.
- Corrección del texto de PLAN-02, PLAN-03 y PLAN-04 —tarea de seguridad,
  criterio de aceptación y bloque de verificación— para que su cadena incluya el
  health.
- Alta de este plan en el índice §9 y en la tabla §8.2 de
  `docs/plans/README.md`.

### Trazabilidad del arreglo

| Origen | Qué se repara |
|---|---|
| PLAN-02 §6 T-05 paso 2 | La lista de rutas públicas omitía Actuator: se corrige el código y el texto del plan |
| PLAN-00 §8 CA-03 | Regresión de un criterio cerrado: `/actuator/health` debe responder `UP` en 9102 |
| ESP-13 §6 | Condición de terminado incumplida por el servicio |
| PLAN-03 §6 T-05 paso 2 | Misma omisión, aún sin ejecutar: se corrige el texto antes de ejecutar |
| PLAN-04 §6 T-06 paso 2 | Misma omisión, aún sin ejecutar: se corrige el texto antes de ejecutar |

### Fuera

- Cualquier cambio en el contrato de login, `me`, claims o vigencia del JWT.
- Cualquier cambio en la configuración de Actuator (`exposure.include`,
  `show-details`).
- Cualquier cambio en el código del gateway, de `catalog-service` o de
  `cash-service`.
- Cambios en `backend/`, `database/` o `docs/openapi/identity.json`.

### Relación con PLAN-02

Este plan no reemplaza ninguna tarea de PLAN-02: completa su T-05 con la ruta que
faltaba y añade un CA. Se ejecuta sobre la rama `feature/identity-service` y el
PR #7 ya abiertos, no sobre `main`. Por eso no declara a PLAN-02 como
dependencia cerrada —sería circular, porque PLAN-02 no puede cerrarse antes de
que este arreglo exista— sino a PLAN-00, cuyo CA-03 es el criterio que se
restaura, y a PLAN-01, ya cerrado, porque CA-09 y CA-12 verifican por el gateway.

### Por qué se corrigen también los planes futuros

El defecto no es del ejecutor: PLAN-02 §6 T-05 paso 2 ordenó exactamente la lista
de rutas que produjo el 401. PLAN-03 §6 T-05 paso 2 y PLAN-04 §6 T-06 paso 2
enumeran otra vez las rutas públicas —`OPTIONS`, Swagger y las reglas de rol—
sin incluir el health. Si se ejecutan tal como están, `catalog-service` y
`cash-service` repetirán el mismo 401 y volverán a romper PLAN-00 CA-03 y la
precondición de PLAN-06 §4 («Health de 9102, 9103 y 9104»).

La corrección es documental y no introduce arquitectura: solo escribe en esos
planes la ruta de observabilidad que ESP-13 §6 ya exige. No se edita
`05-cash-service-tesoreria-reportes.md` porque no declara cadena de seguridad
propia (`T-01` reutiliza la del PLAN-04) y su precondición ya exige PLAN-04
cerrado; queda cubierto por el CA que se añade en PLAN-04.

T-05 es idempotente y no depende del estado de ejecución de esos planes: si
alguno ya estuviera en ejecución o cerrado, la edición del texto se aplica igual
y el CA correspondiente se verifica contra el servicio ya levantado. Por eso este
plan no declara a PLAN-03 ni a PLAN-04 como precondición.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| Rama `feature/identity-service` activa y PR #7 abierto con base `main` que contiene `6df16478` | `git branch --show-current` y `git merge-base --is-ancestor 6df16478 main; echo $LASTEXITCODE` devuelve `0` |
| `identity-service` compila y sus 4 pruebas pasan antes del arreglo | desde `microservices/identity-service`, `mvn test` devuelve `Tests run: 4, Failures: 0, Errors: 0` |
| MySQL 9.7.1 arriba en 3306 y `gestion_auth` con Flyway `V1` y `V2` aplicadas | `SELECT version, success FROM gestion_auth.flyway_schema_history` devuelve dos filas con `success = 1` |
| `JWT_SECRET` exportado en la shell y con al menos 32 caracteres | `$env:JWT_SECRET.Length -ge 32` |
| JDK 21 y Maven 3.9.16 disponibles | `java -version` informa 21 y `mvn -v` informa 3.9.16; si no están en el `PATH`, se usa la ruta de la bitácora de orquestación |
| Puerto 9102 libre antes de arrancar | `Get-NetTCPConnection -LocalPort 9102 -State Listen -ErrorAction SilentlyContinue` sin resultados |
| Gateway arrancable en 9101 y monolito arrancable en 9090, para CA-09 y CA-11 | `curl.exe http://localhost:9101/actuator/health` y login en 9090 responden antes del arreglo |
| `docs/openapi/identity.json` sincronizado con el servicio actual | con el servicio arriba, el `v3/api-docs` en vivo tiene los mismos `paths` que el archivo versionado |
| `DB_USERNAME` y `DB_PASSWORD` exportados en la shell y con permiso sobre `gestion_auth` | `$env:DB_USERNAME` y `$env:DB_PASSWORD` no vacíos; el arranque de T-01 aplica Flyway sin error |
| PLAN-00 y PLAN-01 cerrados | Índice de `docs/plans/README.md` muestra ambos en estado `Cerrado` |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-02 no toca controladores, DTOs ni serialización; CA-07 compara el `v3/api-docs` en vivo con `docs/openapi/identity.json` |
| R-04 Secretos JWT distintos | Alto | T-02 no toca `app.jwt.secret` ni `JwtService`; la verificación usa el `JWT_SECRET` exportado en la shell y no escribe ningún valor |
| R-06 CORS mal configurado | Medio | T-02 no añade configuración de CORS; el servicio sigue sin CORS (ESP-05 §8.1) |
| R-12 Contratos divergentes | Medio | CA-07 y CA-12 demuestran que OpenAPI y Newman no cambiaron |
| R-13 Pérdida del respaldo | Alto | Ninguna tarea escribe bajo `backend/` ni `database/`; CA-13 lo comprueba con el diff acotado |
| Propio: `permitAll` demasiado amplio | Alto | T-02 usa la ruta exacta `/actuator/health`; T-03 prueba que `/actuator/env` exige autenticación y no revela `propertySources` |
| Propio: el filtro JWT sigue delante del health | Bajo | `JwtAuthenticationFilter` solo actúa si llega `Authorization: Bearer`; la sonda de salud no lo envía y CA-01 la ejecuta sin cabecera. Un token inválido en el health sigue devolviendo 401, igual que hoy en toda ruta, y no es una regresión |

## 6. Tareas

### T-01 — Reproducir el 401 del health y fijar la base del arreglo

- **Objetivo:** confirmar el defecto con la salida literal y registrar el commit
  sobre el que se medirá el diff.
- **Especificación:** ESP-13 §6; PLAN-00 §8 CA-03.
- **Archivos:** ninguno.
- **Depende de:** ninguna.
- **Pasos:**
  1. Ejecutar `git rev-parse HEAD` y anotar el hash como **base del arreglo**.
  2. Arrancar `identity-service` en una terminal propia, con `JWT_SECRET`,
     `DB_USERNAME` y `DB_PASSWORD` tomados del entorno, desde
     `microservices/identity-service`, usando `mvn spring-boot:run` (con la ruta
     de Maven de §9 si no está en el `PATH`).
  3. Con el servicio arriba, ejecutar en la terminal de verificación, desde la
     raíz del repositorio:
     `curl.exe -s -o NUL -w "%{http_code}" http://localhost:9102/actuator/health`.
  4. Ejecutar `curl.exe -s http://localhost:9102/actuator/health`.
  5. Abrir `microservices/identity-service/src/main/java/com/cibertec/gestion/identity/security/WebSecurityConfig.java`
     y confirmar que antes de `.anyRequest().authenticated()` solo están
     `POST /api/auth/login`, `OPTIONS /**` y las rutas de Swagger.
- **Verificación:** el comando del paso 3 devuelve `401` y el del paso 4 devuelve
  el cuerpo `{"status":401,"message":"Debe iniciar sesión"}`.
- **Terminado cuando:** las dos salidas coinciden con la evidencia de
  `docs/orquestacion/ejecuciones/02-identity-service-bloqueo.md` y el hash base
  está anotado.

### T-02 — Permitir `/actuator/health` en la cadena de seguridad

- **Objetivo:** que el health responda sin token y que siga siendo la única ruta
  de Actuator abierta.
- **Especificación:** ESP-13 §6; ESP-05 §5.
- **Archivos:**
  - `microservices/identity-service/src/main/java/com/cibertec/gestion/identity/security/WebSecurityConfig.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Añadir `.requestMatchers("/actuator/health").permitAll()` entre el bloque
     de Swagger y `.anyRequest().authenticated()`.
  2. Usar la ruta exacta: no `"/actuator/**"`, no `"/actuator/health/**"` y no
     `EndpointRequest.toAnyEndpoint()`. La regla cubre la ruta de estado y nada
     más; cualquier otro endpoint de Actuator sigue sujeto a
     `.anyRequest().authenticated()`.
  3. No modificar `management.endpoints.web.exposure.include` ni
     `management.endpoint.health.show-details` en
     `microservices/identity-service/src/main/resources/application.properties`.
  4. No tocar `authenticationEntryPoint`, `accessDeniedHandler`, CSRF, política
     de sesión ni el filtro JWT.
  5. Reiniciar el servicio.
- **Verificación:** `curl.exe -s http://localhost:9102/actuator/health` devuelve
  `{"status":"UP"}`; el `git diff` de ese archivo muestra exactamente una línea
  añadida y ninguna eliminada.
- **Terminado cuando:** el health responde 200 y ninguna otra regla de la cadena
  cambió.

### T-03 — Cubrir el health público y el cierre de Actuator con pruebas

- **Objetivo:** que una regresión futura de la cadena se detecte en `mvn test`.
- **Especificación:** ESP-13 §5.1, §6; ESP-05 §5.
- **Archivos:**
  - `microservices/identity-service/src/test/java/com/cibertec/gestion/identity/security/AuthIntegrationTest.java`
- **Depende de:** T-02.
- **Pasos:**
  1. Añadir `healthEsPublicoSinToken()`: `GET /actuator/health` con `MockMvc`
     responde 200, `$.status` vale `UP` y `$.details` no existe. La prueba exige
     el datasource alcanzable, igual que las cuatro pruebas existentes: MySQL
     arriba es precondición de §4.
  2. Añadir `restoDeActuatorNoEstaExpuesto()`: `GET /actuator/env` sin token
     responde 401.
  3. Dentro de `restoDeActuatorNoEstaExpuesto()`, hacer login con
     `admin`/`Admin123*`, extraer el token con
     `JsonPath.read(cuerpo, "$.token")` —jayway ya viene en
     `spring-boot-starter-test`— y repetir `GET /actuator/env` con
     `Authorization: Bearer <token>`: `status().is4xxClientError()` y
     `jsonPath("$.propertySources").doesNotExist()`. Se admite 401 o 404 porque,
     con `anyRequest().authenticated()`, el despacho interno de error puede pasar
     por la cadena; lo que se prohíbe es que el endpoint quede expuesto.
  4. No modificar las cuatro pruebas existentes ni las propiedades
     `app.jwt.secret` y `app.jwt.expiration-ms` de la clase.
  5. No introducir dependencias nuevas en el POM.
- **Verificación:** desde `microservices/identity-service`, `mvn test` devuelve
  `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.
- **Terminado cuando:** las seis pruebas pasan y ninguna prueba existente cambió.

### T-04 — Alinear PLAN-02 con la regla del health

- **Objetivo:** que el plan ejecutado deje de describir una cadena incompleta.
- **Especificación:** ESP-13 §6; PLAN-00 §8 CA-03.
- **Archivos:**
  - `docs/plans/02-identity-service.md`
- **Depende de:** ninguna.
- **Pasos:**
  1. En §6 T-05 paso 2, añadir `/actuator/health` a la lista de rutas públicas.
  2. En §8, añadir al final de la tabla el `CA-13` de PLAN-02: «El health
     responde sin token y no abre el resto de Actuator», con verificación
     `GET /actuator/health` → 200 `{"status":"UP"}` y `GET /actuator/env` sin
     token → 401, y especificación ESP-13 §6.
  3. En §9, añadir la comprobación de ese CA:
     `curl.exe -s http://localhost:9102/actuator/health` y
     `curl.exe -s -o NUL -w "%{http_code}" http://localhost:9102/actuator/env`.
  4. No cambiar el estado del plan, ni los CA-01 a CA-12, ni el resto del texto.
- **Verificación:** `Select-String -Path docs/plans/02-identity-service.md -Pattern '/actuator/health'`
  devuelve al menos cuatro coincidencias: una en el paso 2 de T-05, una en el
  CA-13 y dos en el bloque de §9.
- **Terminado cuando:** T-05 del PLAN-02 y su CA-13 mencionan la ruta y ningún
  otro contenido del plan cambió.

### T-05 — Alinear PLAN-03 y PLAN-04 antes de que se ejecuten

- **Objetivo:** que catálogo y caja nazcan con el health público en su cadena.
- **Especificación:** ESP-13 §6; ESP-05 §5.
- **Archivos:**
  - `docs/plans/03-catalog-service.md`
  - `docs/plans/04-cash-service-cobranza.md`
- **Depende de:** ninguna.
- **Pasos:**
  1. En PLAN-03 §6 T-05 paso 2, añadir `/actuator/health` como público junto a
     `OPTIONS /**` y Swagger.
  2. En PLAN-03 §8, añadir `CA-12`: «El health de 9103 responde `UP` sin token»,
     verificación `curl.exe http://localhost:9103/actuator/health`, especificación
     ESP-13 §6.
  3. En PLAN-03 §9, añadir ese mismo comando como comprobación del CA-12.
  4. En PLAN-04 §6 T-06 paso 2, añadir `/actuator/health` como público en la
     misma lista.
  5. En PLAN-04 §8, añadir `CA-14`: «El health de 9104 responde `UP` sin token»,
     verificación `curl.exe http://localhost:9104/actuator/health`, especificación
     ESP-13 §6.
  6. En PLAN-04 §9, añadir ese mismo comando como comprobación del CA-14.
  7. No editar `docs/plans/05-cash-service-tesoreria-reportes.md`: no declara
     cadena propia y hereda la del PLAN-04.
- **Verificación:**
  `Select-String -Path docs/plans/03-catalog-service.md,docs/plans/04-cash-service-cobranza.md -Pattern '/actuator/health' | Group-Object Path | ForEach-Object { "$($_.Name): $($_.Count)" }`
  informa un mínimo de 3 en cada uno de los dos archivos.
- **Terminado cuando:** ambos planes incluyen la ruta en su tarea de seguridad,
  en su criterio de aceptación y en su verificación.

### T-06 — Re-verificar PLAN-00 CA-03 y cerrar la entrega

- **Objetivo:** demostrar que el criterio original roto vuelve a cumplirse y que
  nada más cambió.
- **Especificación:** ESP-13 §6; PLAN-00 §8 CA-03; ESP-04 §1; ESP-09 §6, §8.
- **Archivos:** ninguno.
- **Depende de:** T-01, T-02, T-03, T-04, T-05.
- **Pasos:**
  1. Ejecutar la verificación completa de §9 de este plan.
  2. Contrastar explícitamente PLAN-00 CA-03: `/actuator/health` de 9102
     responde `UP`.
  3. Comprobar que `git diff --name-only <base-del-arreglo>..HEAD` solo lista las
     rutas de §11.
  4. Actualizar la descripción del PR #7 con la salida de cada criterio.
- **Verificación:** todos los CA de §8 en verde y el PR actualizado.
- **Terminado cuando:** CA-01 a CA-13 se cumplen y el PR #7 describe la
  verificación.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Reproducción y hash base |
| 2 | T-02, T-04, T-05 | Código y documentos son independientes entre sí |
| 3 | T-03 | Requiere la cadena ya corregida |
| 4 | T-06 | Verificación integral |

T-04 y T-05 pueden ejecutarse antes o en paralelo con T-02, pero deben estar
cerradas antes de T-06 para que CA-10 sea verificable.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | El health de identidad responde sin token | `GET http://localhost:9102/actuator/health` → 200 con `{"status":"UP"}` | ESP-13 §6; PLAN-00 CA-03 |
| CA-02 | El health no revela detalles | El cuerpo es exactamente `{"status":"UP"}`: no existe la clave `details` | ESP-13 §6 |
| CA-03 | Ningún otro endpoint de Actuator queda abierto | `GET /actuator/env` sin token → 401; con token válido no devuelve 2xx ni contiene `propertySources` | ESP-05 §5 |
| CA-04 | `/api/**` sigue exigiendo autenticación | `GET /api/auth/me` sin token → 401 con `{"status":401,"message":"Debe iniciar sesión"}` | ESP-05 §3, §5 |
| CA-05 | El login no cambió | `POST /api/auth/login` con `admin` → 200 con `token`, `id`, `usuario`, `nombres`, `apellidos`, `rol` | ESP-04 §5.1 |
| CA-06 | Swagger sigue público | `GET /v3/api-docs` y `GET /swagger-ui/index.html` → 200 sin token | ESP-08 §5; ESP-05 §8.3 |
| CA-07 | El contrato público de identidad no cambió | El `v3/api-docs` en vivo tiene el mismo conjunto de `paths` y el mismo `components.securitySchemes` que `docs/openapi/identity.json`; el archivo versionado no se modifica | ESP-04 §1; ESP-08 §8 |
| CA-08 | Las pruebas pasan, incluidas las nuevas | desde `microservices/identity-service`, `mvn test` → `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` | ESP-13 §5.1 |
| CA-09 | El health del gateway no se alteró | `GET http://localhost:9101/actuator/health` → `{"status":"UP"}` | ESP-07 §7 |
| CA-10 | Los planes 02, 03 y 04 incluyen el health | `Select-String` devuelve `/actuator/health` en la tarea de seguridad, en un CA y en la verificación de cada uno de los tres archivos (tres coincidencias mínimo por archivo) | ESP-13 §6 |
| CA-11 | El monolito sigue siendo ejecutable | `POST http://localhost:9090/api/auth/login` con `admin` → 200 | ESP-11 §10; ESP-13 §6 |
| CA-12 | La colección Postman sigue en verde | `newman run` termina con `failures: 0` y ninguna aserción en rojo; la carpeta `01 · Autenticación` conserva sus 4 peticiones | ESP-09 §6, §8 |
| CA-13 | El diff del arreglo está acotado | `git diff --name-only <base-del-arreglo>..HEAD` solo lista rutas de §11; no aparecen `backend/`, `database/` ni `docs/openapi/identity.json`. Los archivos no versionados ajenos al plan —por ejemplo la evidencia en `docs/orquestacion/`— no cuentan y no se añaden al commit | ESP-11 §10 |

## 9. Verificación

Terminal A — arranque (dejar corriendo):

```powershell
$mvn = 'C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd'
if (-not $env:JWT_SECRET -or $env:JWT_SECRET.Length -lt 32) {
  throw 'Defina JWT_SECRET (>= 32 caracteres) antes de arrancar los servicios'
}
if (-not $env:DB_USERNAME -or -not $env:DB_PASSWORD) {
  throw 'Defina DB_USERNAME y DB_PASSWORD en el entorno'
}
cd microservices/identity-service
& $mvn spring-boot:run
```

Terminal B — verificación, desde la raíz del repositorio:

```powershell
# Las credenciales de las llamadas de verificación son las de demostración de
# ESP-05 §6.1, ya versionadas en esa especificación; no son un secreto nuevo.
# El único secreto real es JWT_SECRET y se lee del entorno, nunca del repositorio.
# Los puertos por defecto son los de ESP-02 §1 (PORT-AUTH = 9102, PORT-GW = 9101,
# PORT-MONO = 9090). Si se exportaron IDENTITY_PORT, GATEWAY_PORT o SERVER_PORT,
# sustituir los literales de los comandos por esos valores; el código no cambia.
$mvn = 'C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd'
$base = '<hash anotado en T-01>'

# CA-01 y CA-02
curl.exe -s -i http://localhost:9102/actuator/health
$health = (curl.exe -s http://localhost:9102/actuator/health | Out-String).Trim()
if ($health -ne '{"status":"UP"}') { throw "Health inesperado: $health" }

# CA-03
$anonCode = (curl.exe -s -o NUL -w "%{http_code}" http://localhost:9102/actuator/env)
if ([int]$anonCode -ne 401) { throw "Actuator anónimo inesperado: $anonCode" }
$token = (curl.exe -s -X POST http://localhost:9102/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token
$envCode = (curl.exe -s -o NUL -w "%{http_code}" http://localhost:9102/actuator/env `
  -H "Authorization: Bearer $token")
if ([int]$envCode -ge 200 -and [int]$envCode -lt 300) { throw "Actuator expuesto: $envCode" }
$envBody = curl.exe -s http://localhost:9102/actuator/env -H "Authorization: Bearer $token" | Out-String
if ($envBody -match 'propertySources') { throw 'Actuator expuesto: /actuator/env devolvió configuración' }

# CA-04
curl.exe -s -i http://localhost:9102/api/auth/me

# CA-05
curl.exe -s -X POST http://localhost:9102/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json |
  Select-Object token, id, usuario, nombres, apellidos, rol

# CA-06
curl.exe -s -o NUL -w "%{http_code}`n" http://localhost:9102/v3/api-docs
curl.exe -s -o NUL -w "%{http_code}`n" http://localhost:9102/swagger-ui/index.html

# CA-07 (comparación estructural: paths y esquema de seguridad)
curl.exe -s http://localhost:9102/v3/api-docs -o $env:TEMP\identity-fix.json
$vivo = Get-Content "$env:TEMP\identity-fix.json" -Raw | ConvertFrom-Json
$repo = Get-Content docs/openapi/identity.json -Raw | ConvertFrom-Json
$pathsVivo = ($vivo.paths.PSObject.Properties.Name | Sort-Object) -join ','
$pathsRepo = ($repo.paths.PSObject.Properties.Name | Sort-Object) -join ','
if ($pathsVivo -ne $pathsRepo) { throw "Paths distintos: $pathsVivo != $pathsRepo" }
$schemesVivo = ($vivo.components.securitySchemes.PSObject.Properties.Name | Sort-Object) -join ','
$schemesRepo = ($repo.components.securitySchemes.PSObject.Properties.Name | Sort-Object) -join ','
if ($schemesVivo -ne $schemesRepo) { throw "SecuritySchemes distintos: $schemesVivo != $schemesRepo" }

# CA-08 (desde la raíz del repositorio; el directorio de trabajo debe ser el
# del servicio para que spring.flyway.locations resuelva ../../database/flyway/auth)
Push-Location microservices/identity-service
& $mvn test
Pop-Location
# Esperado: Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

# CA-09 (con el gateway arrancado)
curl.exe -s http://localhost:9101/actuator/health

# CA-10
Select-String -Path docs/plans/02-identity-service.md,
  docs/plans/03-catalog-service.md,
  docs/plans/04-cash-service-cobranza.md -Pattern '/actuator/health'

# CA-11 (con el monolito arrancado)
curl.exe -s -o NUL -w "%{http_code}`n" -X POST http://localhost:9090/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}'

# CA-12
npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json `
  -e docs/postman/Local_Microservicios.postman_environment.json

# CA-13
git diff --name-only $base..HEAD
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-02 | `git checkout -- microservices/identity-service/src/main/java/com/cibertec/gestion/identity/security/WebSecurityConfig.java`; el health vuelve a 401 y nada más cambia |
| T-03 | Quitar las dos pruebas añadidas a `AuthIntegrationTest.java`; las cuatro originales siguen pasando |
| T-04, T-05 | Revertir las ediciones de PLAN-02, PLAN-03 y PLAN-04; no afecta al código |
| Cualquier punto | `git revert <commit-del-arreglo>` sobre `feature/identity-service`; el PR #7 queda como estaba antes del arreglo |
| Después de fusionar | No hay estado del que depender: el health es observabilidad local, no hay migraciones ni datos, y el gateway no gana rutas. Para volver atrás, un commit nuevo que revierta el cambio de la cadena de seguridad |

No hay `DROP DATABASE`: el arreglo no toca Flyway ni datos.

## 11. Entregables

```text
microservices/identity-service/src/main/java/com/cibertec/gestion/identity/security/WebSecurityConfig.java   (modificado)
microservices/identity-service/src/test/java/com/cibertec/gestion/identity/security/AuthIntegrationTest.java (dos pruebas nuevas)
docs/plans/02-identity-service.md          (T-05 paso 2, CA-13, §9)
docs/plans/03-catalog-service.md           (T-05 paso 2, CA-12, §9)
docs/plans/04-cash-service-cobranza.md     (T-06 paso 2, CA-14, §9)
docs/plans/08-arreglo-health-identity.md   (este plan)
docs/plans/README.md                       (§8.2, §9)
```

Comportamiento observable:

- `GET /actuator/health` de 9102 devuelve 200 `{"status":"UP"}` sin token.
- `GET /actuator/env` de 9102 devuelve 401 sin token; con token válido no
  devuelve 2xx ni el campo `propertySources`.
- Login, `me`, Swagger, OpenAPI y Postman conservan su comportamiento.
- PR #7 con la verificación de CA-01 a CA-13.

## 12. Fuera de alcance

- Editar `docs/especificaciones/05-seguridad-y-jwt.md` para añadir una fila
  `GET /actuator/health | Público` a §5. Es una decisión sobre la especificación:
  se eleva a la orquestadora y no bloquea este plan, porque el comportamiento ya
  está exigido por ESP-13 §6 y PLAN-00 CA-03.
- Abrir `/actuator/**`, `/actuator/info`, `/actuator/env` o cualquier otro
  endpoint, y cambiar `show-details`.
- Añadir rutas de Actuator al gateway: `/actuator/health` de cada servicio se
  consulta en su propio puerto, no a través de 9101.
- Añadir una petición de health de identidad a la colección Postman: el health
  del gateway ya está cubierto en `00 · Salud` y ESP-09 §4.1 no lo exige por
  servicio.
- Tocar `catalog-service` o `cash-service` en código: su corrección es de plan y
  se aplicará cuando se ejecuten.
- Editar `05-cash-service-tesoreria-reportes.md`.
- Cambiar el contrato, los DTOs, el JWT, la colección Postman o el archivo
  `docs/openapi/identity.json`.
- Migraciones, seeds, `database/crear_base.sql` y `backend/`.
- `.opencode/` y `docs/orquestacion/bitacora.md`: los gestiona la orquestadora.
- Cualquier cambio de arquitectura, puertos o variables de entorno.
