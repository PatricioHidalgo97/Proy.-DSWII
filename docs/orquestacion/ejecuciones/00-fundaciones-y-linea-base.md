# Ejecución PLAN-00 — Fundaciones, correcciones documentales y línea base del contrato

Registro crudo del ciclo. El subagente `ejecutor` completó el trabajo en la rama
pero **devolvió la sesión sin informe escrito y sin hacer push ni abrir el PR**.
Todo lo que sigue es la evaluación independiente del orquestador sobre el
repositorio, no un resumen de un informe del ejecutor.

## Estado de partida

- `main` en `20cc6edc`, sincronizado con `origin/main`.
- Los ocho planes versionados por PR #4.
- Herramientas fuera del PATH; rutas usadas:
  - JDK 21: `C:\Program Files\Java\jdk-21.0.11`
  - Maven 3.9.16: `C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin`
  - MySQL 9.7.1: `C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe`, `root`/`admin`

## Rama y commits

Rama: `feature/microservicios-api-gateway` (partió de `main`, no de la rama
remota antigua, que estaba por detrás).

| Commit | Mensaje |
|---|---|
| `046c5c10` | feat(microservices): crear estructura de servicios, .env.example y POMs independientes |
| `fe6ca40d` | feat(microservices): agregar clases de arranque y configuracion base de los tres servicios |
| `e4d4e115` | chore: versionar .gitignore y dejar de rastrear node_modules, target y data |
| `30610084` | docs(esp-06): corregir la ruta de Flyway a ../../database/flyway/{servicio} |
| `e4a3b816` | docs(contrato): fijar la linea base del contrato y crear el esqueleto de Postman |
| `811ecf3a` | docs(planes): marcar PLAN-00 como cerrado en el indice |

PR: #5 → merge commit `1c01c2bb`.

## Tareas

| Tarea | Estado | Evidencia |
|---|---|---|
| T-01 puertos | Hecho | `netstat -ano \| grep -E ":(9101\|9102\|9103\|9104\|4400) "` sin salida |
| T-02 estructura y `.env.example` | Hecho | 4 archivos existen; `.env` ignorado |
| T-03 tres POM | Hecho | `mvn -q -f microservices/<svc>/pom.xml -DskipTests package` → exit 0 ×3 |
| T-04 arranque y config | Hecho | `/actuator/health` → `{"status":"UP"}` en 9102, 9103, 9104 |
| T-05 ruta Flyway ESP-06 | Hecho | `filesystem:../../database/flyway/{servicio}` en línea 40; §3.1 justifica |
| T-06 bases y migraciones | Hecho | 9 conteos correctos, 2 integridades en 0, correlativos 216/24, 0 filas `success=0` |
| T-07 `.gitignore` | Hecho | `git ls-files \| grep -c node_modules` = 0; directorios siguen en disco |
| T-08 línea base contrato | Hecho con desviación | 6 formas, error, 52 endpoints, inventario real 26 (ver abajo) |
| T-09 esqueleto Postman | Hecho | JSON válido, schema v2.1.0, carpetas `00 · Salud` y `01 · Autenticación` |

## Criterios de aceptación

| CA | Estado | Evidencia |
|---|---|---|
| CA-01 | Cumple | 3 builds exit 0 |
| CA-02 | Cumple | `grep -c "<parent>"` = 1 en cada POM |
| CA-03 | Cumple | health UP en 9102/9103/9104 |
| CA-04 | Cumple | `success=0` → 0,0,0; historial auth=2, catalog=4, cash=4 |
| CA-05 | Cumple | 4, 24, 8, 26, 12, 5, 344, 216, 24 |
| CA-06 | Cumple | integridad 0 y 0 |
| CA-07 | Cumple | `ls ../../database/flyway/auth` desde el servicio lista V1 y V2 |
| CA-08 | Cumple | `git diff --stat main..HEAD -- backend/src database/crear_base.sql database/flyway` vacío |
| CA-09 | Cumple | `docs/contract/baseline-contrato.md`: 52 filas de endpoint, 6 formas `### 1.1–1.6` |
| CA-10 | Cumple (parcial) | Newman parsea y ejecuta; falla solo por `ECONNREFUSED 127.0.0.1:9101` (gateway inexistente hasta PLAN-01) |
| CA-11 | Cumple | `git ls-files \| grep -cE "\.env$"` = 0 |

## Regresión del monolito

`mvn -q -f backend/pom.xml -DskipTests compile` → exit 0. `backend/` sigue
ejecutable.

## Desviaciones y hallazgos

### D-01 — El plan declara «32 ítems» y la colección real tiene 26

PLAN-00 §T-08 paso 4 pide registrar «`docs/Sistema_Gestion.postman_collection.json`,
32 ítems». Verificación con node sobre el archivo versionado:

```text
carpetas: 5 [ '1. Autenticación', '2. Catálogos', '3. Servicios y cuentas',
              '4. Tesorería', '5. Reportes XLSX' ]
peticiones: 26
items top-level: 5
```

No hay otra colección en el repositorio ni en `node_modules`. El ejecutor no
inventó elementos: transcribió las 26 reales y dejó la desviación anotada en
`docs/contract/baseline-contrato.md` §4. **Decisión del orquestador:** no es
motivo de bloqueo del plan —el criterio CA-09 verificable es «52 endpoints y 6
formas», que sí se cumple—, pero queda registrada para que un humano decida si
se corrige el plan o si falta una colección ampliada.

### D-02 — El `chore:` de T-07 parece una eliminación masiva

`git rm -r --cached` deja el commit con ~22 430 archivos eliminados del índice
(3 051 748 líneas). Comprobado que **no borra nada del disco**:
`ls -d backend/target backend/data frontend/node_modules` → los tres existen.
`git status --short backend database/crear_base.sql` vacío.

### D-03 — El ejecutor no entregó informe ni abrió PR

Detectado al evaluar: los cinco commits estaban solo en local y
`git ls-remote` no tenía la rama actualizada. El orquestador hizo push, abrió
el PR #5 y lo fusionó tras verificar CA-01..CA-11 por cuenta propia.

### D-04 — Newman reporta fallos esperados

Los 4 fallos son consecuencia de que el gateway (9101) no existe todavía. El
ítem de salud está protegido por `pm.execution.skipRequest()` mientras
`gatewayReady != 'true'`; el de login sí intenta `{{baseUrl}}` y por eso falla.
Es el comportamiento declarado por el propio plan («dejar el ítem marcado como
pendiente hasta PLAN-01»).
