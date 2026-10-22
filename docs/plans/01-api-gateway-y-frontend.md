# PLAN-01 — API Gateway y traslado del frontend al gateway

## 1. Objetivo

Poner en marcha el `api-gateway` como único punto de entrada del frontend,
enrutando por prefijo a los servicios que existan y al monolito lo que todavía
no se haya extraído, y cambiar el `apiUrl` de Angular al gateway.

Este plan ejecuta el **Estado 1** de ESP-11: tras cerrarlo, el sistema completo
funciona con el frontend apuntando al gateway, aunque catálogo y caja todavía
resuelvan contra el monolito.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-07 | §1, §2, §3, §4, §5, §6, §7, §8, §10, §11 | Gateway MVC, rutas, CORS, Actuator, errores |
| ESP-04 | §3 | Los diez prefijos y su servicio dueño |
| ESP-10 | §1, §3.1, §3.2, §3.3, §4 | `apiUrl` y puerto 4400 |
| ESP-11 | §2.2, §3 (F1), §7 (Fase 1), §8 | Estado 1, criterios de salida, reversa |
| ESP-05 | §8 | CORS solo en el gateway |
| ESP-02 | §1, §3, §5 | Puerto y variables |
| ESP-12 | R-06, R-07, R-11, R-14 | Riesgos aplicables |
| ESP-13 | §1.1, §2, §9 | Nomenclatura y control de versiones |

## 3. Alcance

### Dentro

- Proyecto `microservices/api-gateway` con `spring-cloud-starter-gateway-server-webmvc`.
- Rutas de los diez prefijos de ESP-04 §3.
- Ruta transitoria al monolito para los prefijos aún no extraídos.
- CORS centralizado con `Content-Disposition` expuesto.
- Actuator con `health` y `gateway`.
- Reenvío intacto del header `Authorization`.
- Cambio de `frontend/src/environments/environment.ts`.
- Puerto `4400` en `frontend/angular.json`.
- Carpetas `00 · Salud` y `01 · Autenticación` operativas en la colección Postman.

### Fuera

- Identidad, catálogo y caja (PLAN-02 a PLAN-05).
- Validación de JWT en el gateway.
- Swagger agregado (PLAN-06).
- Cualquier cambio de lógica en el frontend.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-00 cerrado | Índice de `docs/plans/README.md` |
| `microservices/` existe con los tres esqueletos | `ls microservices` |
| El monolito arranca en 9090 | `mvn -f backend/pom.xml spring-boot:run` y `curl localhost:9090/actuator/health` o login |
| Puerto 9101 libre | ESP-02 §8 |
| Este plan resuelve la contradicción ESP-07 §4.1 / ESP-11 §2.2 | Ver T-02 |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-06 CORS mal configurado | Medio | T-03 configura CORS solo en el gateway y expone `Content-Disposition`; CA-06 lo comprueba descargando un XLSX del monolito |
| R-07 Puertos ocupados | Medio | T-01 verifica 9101 y 4400; ambos quedan en variables |
| R-11 Big bang | Alto | T-02 deja la ruta al monolito activa; al cerrar el plan el sistema completo funciona |
| R-14 Sobrediseño | Medio | Sin `StripPrefix`, sin filtros propios, sin reintentos, sin seguridad |

## 6. Tareas

### T-01 — Crear el proyecto del gateway

- **Objetivo:** un proyecto Spring Boot MVC con Spring Cloud Gateway.
- **Especificación:** ESP-07 §1, §2, §5.
- **Archivos:**
  - `microservices/api-gateway/pom.xml`
  - `microservices/api-gateway/.env.example`
  - `microservices/api-gateway/src/main/java/com/cibertec/gestion/gateway/ApiGatewayApplication.java`
  - `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** PLAN-00.
- **Pasos:**
  1. Declarar `spring-cloud-starter-gateway-server-webmvc` y `spring-boot-starter-actuator`.
  2. Importar el BOM `spring-cloud-dependencies` en la versión **2025.0.x**, que es la línea compatible con Spring Boot 3.5.x según la tabla oficial de Spring Cloud. No usar 2025.1.x: esa línea exige Boot 4.
  3. Fijar el número exacto del service release en el POM al momento de ejecutar la tarea.
  4. Declarar `server.port=${GATEWAY_PORT:9101}`.
  5. Declarar `spring.application.name=api-gateway`.
  6. Exponer solo `health` y `gateway` en Actuator.
- **Verificación:** `mvn -q -f microservices/api-gateway/pom.xml -DskipTests package` termina sin error.
- **Terminado cuando:** el proyecto empaqueta y arranca en 9101.

### T-02 — Definir las rutas y la ruta transitoria al monolito

- **Objetivo:** enrutar los diez prefijos y dejar activo el respaldo al monolito.
- **Especificación:** ESP-07 §4, §4.1; ESP-11 §2.2.
- **Archivos:** `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** T-01.
- **Pasos:**
  1. Declarar las tres rutas de ESP-07 §5 con `webmvc.routes`.
  2. No usar `StripPrefix`.
  3. Declarar una cuarta ruta `monolito` con `Path=/api/**` y `uri=${MONOLITO_URL:http://localhost:9090}`.
  4. Ordenar las rutas de modo que `monolito` sea la **última**, con `order` mayor que las otras tres.
  5. Documentar en el propio archivo, como comentario, que esa ruta se elimina en PLAN-05.
  6. **Actualizar ESP-07 §4.1** para que la regla "toda ruta no listada devuelve 404" diga: "toda ruta no listada devuelve 404 **salvo durante la migración**, donde la ruta `monolito` la absorbe; esa ruta se retira al cerrar la fase 3".
- **Verificación:** `GET http://localhost:9101/api/socios` sin token devuelve el 401 del monolito, no un 404 del gateway.
- **Terminado cuando:** los diez prefijos responden y el 404 solo aparece con la ruta `monolito` desactivada.

### T-03 — Configurar CORS

- **Objetivo:** que el navegador pueda llamar al gateway y leer el nombre de los XLSX.
- **Especificación:** ESP-07 §6; ESP-05 §8.
- **Archivos:** `microservices/api-gateway/src/main/java/com/cibertec/gestion/gateway/config/CorsConfig.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Registrar un `CorsConfigurationSource` para `/**`.
  2. Orígenes desde `${CORS_ORIGINS:http://localhost:4400}`.
  3. Métodos `GET,POST,PUT,DELETE,OPTIONS`; headers `Authorization,Content-Type`.
  4. Exponer `Content-Disposition`.
  5. No habilitar credenciales.
  6. Confirmar que ningún servicio de dominio configura CORS propio.
- **Verificación:** `curl -i -X OPTIONS http://localhost:9101/api/socios -H "Origin: http://localhost:4400" -H "Access-Control-Request-Method: GET"` devuelve los headers CORS.
- **Terminado cuando:** el preflight responde con origen, métodos y `Content-Disposition` expuesto.

### T-04 — Verificar la propagación de `Authorization`

- **Objetivo:** comprobar que el gateway no altera el token.
- **Especificación:** ESP-07 §3.1.
- **Archivos:** ninguno.
- **Depende de:** T-02.
- **Pasos:**
  1. Obtener un token del monolito con `POST http://localhost:9090/api/auth/login`.
  2. Llamar `GET http://localhost:9101/api/socios` con ese token.
  3. Comparar el resultado con la llamada directa al monolito.
- **Verificación:** ambas llamadas devuelven 200 y el mismo JSON.
- **Terminado cuando:** el gateway devuelve exactamente lo mismo que el monolito.

### T-05 — Cambiar el frontend al gateway

- **Objetivo:** que Angular consuma el gateway sin tocar lógica.
- **Especificación:** ESP-10 §1, §3.1, §3.2, §3.3.
- **Archivos:**
  - `frontend/src/environments/environment.ts`
  - `frontend/angular.json`
- **Depende de:** T-02, T-03.
- **Pasos:**
  1. Cambiar `apiUrl` a `'http://localhost:9101/api'`.
  2. Agregar `"options": { "port": 4400 }` en `architect.serve`.
  3. No tocar `app.routes.ts`, `app.html`, `auth.interceptor.ts`, `auth.guard.ts`, `auth.service.ts`, ningún `*.service.ts` ni `pages/*`.
- **Verificación:** `git status --short frontend` reporta solo esos dos archivos.
- **Terminado cuando:** solo cambian los dos archivos declarados.

### T-06 — Completar la carpeta de salud en Postman

- **Objetivo:** que la colección verifique el gateway.
- **Especificación:** ESP-09 §3, §4.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-02.
- **Pasos:**
  1. Agregar la variable de entorno `gatewayHealthUrl` con valor `http://localhost:9101/actuator/health`.
  2. Agregar en `00 · Salud` un `GET {{gatewayHealthUrl}}`.
  3. Verificar 200 y `status = UP`.
  4. Mantener el login de `01 · Autenticación` funcionando a través del gateway.
- **Verificación:** `npx newman run` ejecuta ambas carpetas en verde.
- **Terminado cuando:** salud y login pasan por el gateway.

### T-07 — Documentar la operación local

- **Objetivo:** dejar escrito cómo levantar el sistema en el Estado 1.
- **Especificación:** ESP-07 §10; ESP-06 §6.2.
- **Archivos:** `microservices/README.md`
- **Depende de:** T-05.
- **Pasos:**
  1. Documentar el orden: monolito, gateway, frontend.
  2. Documentar la reversa: `$env:GATEWAY_PORT`, `MONOLITO_URL` y volver `apiUrl` a 9090.
  3. Registrar qué prefijos ya van al gateway y cuáles al monolito.
- **Verificación:** el README permite reproducir el Estado 1 sin consultar otra fuente.
- **Terminado cuando:** el README describe el estado real del repositorio.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Proyecto y arranque |
| 2 | T-02, T-03 | Rutas y CORS son independientes entre sí |
| 3 | T-04, T-06 | Requieren T-02; T-06 requiere además T-03 |
| 4 | T-05 | Requiere T-02 y T-03 |
| 5 | T-07 | Documenta el estado que deja T-05 |

T-05 es la única tarea que modifica `frontend/`.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | El gateway arranca en 9101 | `/actuator/health` responde `UP` | ESP-07 §7 |
| CA-02 | Las diez rutas están activas | `/actuator/gateway/routes` devuelve once entradas (diez prefijos + monolito) | ESP-07 §4 |
| CA-03 | Todo el sistema funciona con el frontend en el gateway | Login y las once pantallas cargan por 9101 | ESP-11 §7 Fase 1 |
| CA-04 | El token atraviesa el gateway sin cambios | La misma petición por 9101 y por 9090 devuelve el mismo JSON | ESP-07 §3.1 |
| CA-05 | CORS responde solo desde el gateway | Preflight en 9101 devuelve headers; preflight en 9102–9104 no los devuelve | ESP-05 §8.1 |
| CA-06 | `Content-Disposition` es legible por el navegador | La descarga de un XLSX conserva el nombre del archivo | ESP-07 §6 |
| CA-07 | Una ruta inexistente devuelve 404 del gateway | `GET http://localhost:9101/api/inexistente` con la ruta monolito desactivada | ESP-07 §4.1 |
| CA-08 | El frontend solo cambió en dos archivos | `git status --short frontend` | ESP-10 §3.3 |
| CA-09 | El monolito sigue siendo ejecutable | Arranca en 9090 y responde login | ESP-11 §9 |
| CA-10 | La colección ejecuta salud y login en verde | `npx newman run` | ESP-09 §4 |

## 9. Verificación

```powershell
# Arranque
cd backend                       ; mvn spring-boot:run     # 9090
cd ../microservices/api-gateway  ; mvn spring-boot:run     # 9101
cd ../../frontend                ; npm start -- --port 4400

# CA-01
curl.exe http://localhost:9101/actuator/health

# CA-02
curl.exe http://localhost:9101/actuator/gateway/routes

# CA-04
$token = (curl.exe -s -X POST http://localhost:9090/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token
curl.exe -s http://localhost:9101/api/socios -H "Authorization: Bearer $token"
curl.exe -s http://localhost:9090/api/socios -H "Authorization: Bearer $token"

# CA-05
curl.exe -i -X OPTIONS http://localhost:9101/api/socios `
  -H "Origin: http://localhost:4400" -H "Access-Control-Request-Method: GET"

# CA-08
git status --short frontend

# CA-10
npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json `
  -e docs/postman/Local_Microservicios.postman_environment.json
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-02 / T-03 | Detener el gateway; el frontend sigue funcionando si `apiUrl` vuelve a 9090 |
| T-05 | `git checkout -- frontend/src/environments/environment.ts frontend/angular.json` |
| Cualquier punto | El monolito nunca se modifica, así que el Estado 0 siempre es reproducible |

La reversa es de configuración, no de código, tal como exige ESP-11 §8.

## 11. Entregables

```text
microservices/api-gateway/
├─ .env.example
├─ pom.xml
└─ src/main/
   ├─ java/com/cibertec/gestion/gateway/
   │  ├─ ApiGatewayApplication.java
   │  └─ config/CorsConfig.java
   └─ resources/application.properties
frontend/src/environments/environment.ts     (modificado)
frontend/angular.json                        (modificado)
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json (2 carpetas)
docs/especificaciones/07-api-gateway.md      (actualizado en §4.1)
```

## 12. Fuera de alcance

- Validación de JWT y reglas de rol en el gateway.
- Cualquier ruta hacia servicios que aún no existen.
- `StripPrefix`, filtros propios, reintentos o caché.
- Swagger agregado.
- Cambios en componentes, rutas o servicios de Angular.
