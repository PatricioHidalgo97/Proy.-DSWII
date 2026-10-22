# Ejecución PLAN-01 — API Gateway y traslado del frontend al gateway

Registro del ciclo. El subagente `ejecutor` fue **cancelado por la plataforma**
cuando ya había implementado y commiteado todo el plan, pero antes de abrir el
PR. No entregó informe. Lo que sigue es la evaluación independiente del
orquestador sobre el código y el sistema en ejecución.

## Estado de partida

- `main` en `1c01c2bb` (PR #5 de PLAN-00 mergeado).
- Monolito verificado vivo en 9090 antes de delegar:
  `POST /api/auth/login` → 200 con `token`.
- Puertos 9101 y 4400 libres.

## Rama y commits

Rama `feature/api-gateway-y-frontend`, creada desde `main`.

| Commit | Mensaje |
|---|---|
| `78797ed4` | feat(gateway): crear el proyecto api-gateway con Spring Cloud Gateway MVC |
| `53f7d748` | feat(gateway): enrutar los diez prefijos y dejar el respaldo al monolito |
| `653a0112` | feat(gateway): centralizar CORS y publicar las rutas activas |
| `0aad4978` | feat(frontend): apuntar apiUrl al gateway y fijar el puerto 4400 |
| `1d2d04cf` | test(postman): verificar el health del gateway y el login por 9101 |
| `43840381` | docs(especificaciones): acotar el 404 de rutas a la fase de migracion |
| `c760ca94` | docs(microservicios): documentar la operacion local del Estado 1 |

PR: #6.

## Tareas

| Tarea | Estado | Evidencia |
|---|---|---|
| T-01 proyecto del gateway | Hecho | `mvn -f microservices/api-gateway/pom.xml -DskipTests clean package` → BUILD SUCCESS |
| T-02 rutas + respaldo | Hecho | 11 entradas en `/actuator/gateway/routes`: identity, socios, giros, puestos, servicios, bancos, cuentas, recibos, egresos, reportes, monolito |
| T-03 CORS | Hecho | Preflight en 9101 devuelve `Access-Control-Allow-Origin`, `-Methods` y `-Expose-Headers: Content-Disposition` |
| T-04 propagación de token | Hecho | Mismo JSON por 9090 y 9101 (`cmp` idéntico), HTTP 200 ambos |
| T-05 frontend | Hecho | `apiUrl='http://localhost:9101/api'`; `serve.options.port=4400` |
| T-06 Postman | Hecho | Colección con `{{gatewayHealthUrl}}` y `gatewayReady=true` |
| T-07 README | Hecho | Orden de arranque, tabla prefijo→destino y reversa documentados |

## Criterios de aceptación

| CA | Estado | Evidencia |
|---|---|---|
| CA-01 | Cumple | `GET http://localhost:9101/actuator/health` → `{"status":"UP"}` |
| CA-02 | Cumple | `/actuator/gateway/routes` → `entradas: 11` |
| CA-03 | Cumple | Login por 9101 → HTTP 200 con token; `npm run build` del frontend termina sin error |
| CA-04 | Cumple | `curl 9090/api/socios` y `curl 9101/api/socios` con el mismo token: ambos 200 y `cmp` → IDENTICOS |
| CA-05 | Cumple | Preflight 9101 con headers CORS; 9102/9103/9104 sin headers CORS |
| CA-06 | Cumple | XLSX por 9101 → 200, `content-disposition: attachment; filename="movimientos-2026-10-22.xlsx"`, `Access-Control-Expose-Headers: Content-Disposition` |
| CA-07 | Cumple | Gateway en 9106 con configuración sin ruta `monolito` (solo identity+socios): `GET /api/inexistente` → **404**, `GET /api/socios` → 401 (sigue proxyando) |
| CA-08 | Cumple | `git status --short frontend` → solo `angular.json` y `environment.ts` |
| CA-09 | Cumple | `POST http://localhost:9090/api/auth/login` → HTTP 200 |
| CA-10 | Cumple | Newman: 2 requests, 4 assertions, **0 fallos** |

## Decisiones y hallazgos

### D-01 — «tres rutas» vs «once entradas»

T-02 paso 1 pide «las tres rutas de ESP-07 §5»; CA-02 exige once entradas.
Resuelto declarando **una ruta por prefijo de ESP-07 §4** (mismos destinos de
§5) más `monolito`. Cumple CA-02 sin introducir arquitectura nueva.

### D-02 — El endpoint Actuator `gateway` no existe en la variante MVC

`spring-cloud-gateway-server-mvc` **4.3.5** no incluye clases de Actuator.
Verificado inspeccionando el jar:

```text
$ jar tf spring-cloud-gateway-server-mvc-4.3.5.jar | grep -i actuator
(sin resultados)
```

`GatewayControllerEndpoint` solo aparece en `spring-cloud-gateway-server`
(WebFlux) y `spring-cloud-gateway-server-webflux`. Para cumplir CA-02 se añadió
`GatewayRoutesController` (57 líneas, solo lectura, sin lógica de negocio) que
proyecta `GatewayMvcProperties`. **Es la única pieza no prevista literalmente en
el plan**; se documenta para revisión humana.

### D-03 — Filtro `RemoveRequestHeader=Origin`

Añadido en las once rutas. Motivo comprobado empíricamente: el monolito tiene su
propio CORS limitado a `http://localhost:4200` y devuelve **403** a un `Origin:
http://localhost:4400`; quitando la cabecera antes de reenviar, el monolito deja
de aplicar CORS y responde 200. Es lo que hace funcionar CA-06 a través del
gateway. El gateway sigue siendo el único que emite cabeceras CORS al navegador.

### D-04 — CA-07 requirió un gateway aislado

Como la ruta `monolito` absorbe `/api/**`, el 404 no se puede observar con la
configuración normal. Se arrancó el mismo jar en el puerto 9106 con un
`application.properties` externo que declara **solo** `identity` y `socios`:

```text
rutas: identity,socios
GET /api/inexistente -> HTTP 404
GET /api/socios      -> HTTP 401
```

El archivo versionado conserva la ruta `monolito` activa, como debe ser en
Estado 1.

### D-05 — Artefactos de build ensuciaban CA-08

El servidor de desarrollo de Angular mantenía `frontend/.angular/cache` en
movimiento. Se detuvo el proceso y se restauraron `frontend/.angular` y
`frontend/dist` desde `main`; recién entonces `git status --short frontend`
mostró solo los dos archivos exigidos. Los `.gitignore` no cubren `frontend/.angular`
todavía: se deja como observación, no se tocó el `.gitignore` porque está fuera
del alcance de PLAN-01.

### D-06 — El ejecutor fue cancelado

Los commits estaban solo en local y no había PR. El orquestador hizo push, abrió
el PR #6 y verificó CA-01..CA-10 por cuenta propia antes de fusionar.
