# Bitácora de orquestación

Estado persistente del agente `orquestador`. Sin este archivo no es posible
retomar un plan a medias entre sesiones.

## Estado actual

- Plan en curso: PLAN-06 — Contrato consolidado y Swagger agregado
- Rama de trabajo: por crear (`feature/contrato-y-swagger-agregado`) desde `main`
- PR: sin PR
- Punto exacto: inicio del plan
- Siguiente acción: ejecutar el barrido de coherencia de PLAN-06 y PLAN-07 antes
  de delegar, luego delegar PLAN-06 al ejecutor.

## Planos cerrados

| Plan | PR | Merge | Nota |
|---|---|---|---|
| PLAN-00 Fundaciones | #5 | `1c01c2bb` | CA-01..CA-11. Desviación D-01: la colección real tiene 26 peticiones, no 32 |
| PLAN-01 Gateway | #6 | `6df16478` | CA-01..CA-10. `GatewayRoutesController` propio: Gateway MVC no publica `/actuator/gateway/routes` |
| PLAN-02 identity | #7 | `55c7709e` | CA-01..CA-12. **Bloqueo**: health devolvía 401 |
| PLAN-08 health | #7 | `55c7709e` | Arreglo de una línea + 2 pruebas + alineación de planes 02/03/04. CA-01..CA-13 |
| PLAN-03 catalog | #8 | `8b9397e3` | CA-01..CA-12. `@Column(name="cargo_a")` mapea el esquema Flyway sin tocar la migración |
| PLAN-04 cobranza | #9 | `0998dc76` | CA-01..CA-14. **Bloqueo**: CA-02 pedía ids que el esquema no tenía |
| PLAN-09 ids anidados | #9 | `0998dc76` | Migración `V5` + backfill. CA-01..CA-16 |
| PLAN-05 tesorería | #10 | `deefdd99` | CA-01..CA-12. Ruta `monolito` **retirada**: punto de no retorno del strangler |

## Planes pendientes

| Plan | Depende de | Nota |
|---|---|---|
| PLAN-06 contrato y Swagger | PLAN-02, PLAN-03, PLAN-05 | Los tres servicios ya exportan su OpenAPI |
| PLAN-07 Postman, regresión y demo | PLAN-06 | |

## Archivos de ejecución

Cada plan tiene su acta en `docs/orquestacion/ejecuciones/`. Los informes crudos
viven ahí; no se resumen.

## Agentes

| Agente | Modelo | Rol |
|---|---|---|
| `ejecutor` | deepseek-v4.1-flash-1017#max | Implementa planes. Informe **escrito en disco** antes de responder |
| `planificador-arreglos` | deepseek-v4.1-flash-1017#max | Escribe planes de arreglo. Barrido de coherencia previo |
| `verificador` | deepseek-v4.1-flash-1017#max | **Nuevo.** Audita la entrega; no confía en el informe del ejecutor |

### Cambios de agente tras analizar los bloqueos

1. `ejecutor`: el informe se escribe en `docs/orquestacion/ejecuciones/` antes de
   responder, para que una sesión caída no lo pierda. Se sube a `#max`.
2. `ejecutor`: los planes de más de diez tareas se ejecutan por rangos; si se
   corta, deja el punto exacto y commitea lo terminado.
3. `planificador-arreglos`: barrido de coherencia obligatorio antes de escribir
   el plan (criterios realizables, tareas verificables, no romper planes cerrados).
4. `verificador` creado: verificación independiente, con acta propia.

## Entorno (NO está en el PATH)

| Herramienta | Ruta |
|---|---|
| JDK 21 | `C:\Program Files\Java\jdk-21.0.11` |
| Maven 3.9.16 | `C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin` |
| MySQL 9.7.1 | `C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe`, `root`/`admin` |
| Monolito | 9090, `admin`/`Admin123*` |

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.11"
export PATH="$JAVA_HOME/bin:/c/Users/juan2/.m2/wrapper/dists/apache-maven-3.9.16/56ba1f9f/bin:$PATH"
export DB_USERNAME=root DB_PASSWORD=admin
export JWT_SECRET="SistemaGestionCibertec2026ClaveSeguraJWT123456789"
```

## Notas operativas que ya han costado un ciclo

- **Jar en uso**: si `mvn package` falla con `Unable to rename ... .jar.original`,
  mata el proceso que lo tiene abierto y borra `target/*.jar`. Si no, el jar
  queda sin manifest (`no main manifest attribute`) y no arranca.
- **MySQL no está en el PATH**: usa la ruta absoluta del cliente.
- **`/tmp` en Git Bash** mapea a `C:\Users\juan2\AppData\Local\Temp`. Un path
  `file:/tmp/...` pasado a Java en Windows **no** resuelve: usa
  `file:C:/Users/juan2/AppData/Local/Temp/...`.
- **Newman acumula documentos** en `gestion_cash` en cada corrida (ESP-09 §8 lo
  permite). Para volver al estado seed: `DROP DATABASE gestion_cash` +
  reaplicar `database/crear_base_microservicios.sql` + arrancar cash.
- **`JWT_SECRET` idéntico** en monolito, identity, catalog y cash, o los tokens
  no se aceptan entre servicios.
- **Los servicios de dominio no tienen CORS**: el gateway lo centraliza y quita
  el header `Origin` antes de reenviar. Por eso el monolito, que sí tiene CORS
  propio, acepta las peticiones del gateway.
- **`GET /api/egresos` sin `periodo`** devuelve 401 (no 400): el error de
  parámetro faltante cae en el despacho de error del filtro de seguridad.

## Estado de la base al cerrar PLAN-05

`gestion_cash`: 344 cuentas, 216 recibos, 24 egresos, `RECIBO=216`, `EGRESO=24`,
migraciones `V1..V5` con `success=1`. `gestion_catalog`: 24 socios, 8 giros,
26 puestos, 12 servicios, 5 bancos. `gestion_auth`: 4 usuarios.

## Historial

| Ciclo | Plan | Resultado | PR | Merge | Notas |
|---|---|---|---|---|---|
| 0 | — | Cerrado | #4 | Sí | Planes versionados e índice §9 |
| 1 | PLAN-00 | Cerrado | #5 | Sí | D-01: la colección real tiene 26 peticiones |
| 2 | PLAN-01 | Cerrado | #6 | Sí | Gateway MVC sin endpoint actuator nativo |
| 3 | PLAN-02 | Cerrado con arreglo | #7 | Sí | Bloqueo: health 401 |
| 4 | PLAN-08 | Cerrado | #7 | Sí | Arreglo de una línea, 6 pruebas |
| 5 | PLAN-03 | Cerrado | #8 | Sí | `cargo_a` mapeado en la entidad |
| 6 | PLAN-04 | Cerrado con arreglo | #9 | Sí | Bloqueo: CA-02 imposible con el esquema |
| 7 | PLAN-09 | Cerrado | #9 | Sí | Migración `V5` con backfill |
| 8 | PLAN-05 | Cerrado | #10 | Sí | Ruta `monolito` retirada; 52 endpoints en microservicios |
| 9 | — | Agentes | — | — | `verificador` creado; ejecutor y planificador reforzados |
