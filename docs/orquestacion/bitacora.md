# Bitácora de orquestación

Estado persistente del agente `orquestador`. Sin este archivo no es posible
retomar un plan a medias entre sesiones.

## Estado actual

- Plan en curso: PLAN-03 — catalog-service
- Rama de trabajo: por crear (`feature/catalog-service`)
- PR: sin PR
- Punto exacto: inicio del plan
- Siguiente acción: delegar PLAN-03 al ejecutor.

## Entorno de ejecución (no está en PATH)

| Herramienta | Ruta / valor |
|---|---|
| JDK 21 | `C:\Program Files\Java\jdk-21.0.11` |
| Maven 3.9.16 | `C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin` |
| MySQL 9.7.1 | `C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe`, `root`/`admin` |
| Monolito | 9090, `admin`/`Admin123*` |
| Gateway | 9101 |
| identity-service | 9102, `JWT_SECRET` obligatorio y sin default |
| Alto total de logs | el jar no se puede reempaquetar mientras el proceso lo tenga abierto: matar el proceso y borrar `target/*.jar` antes de `mvn package` |

## Notas operativas

- `JWT_SECRET` debe ser **idéntico** en monolito e identity para que CA-05 pase.
  Se exporta solo en la shell; nunca se versiona.
- El health de cada servicio debe ser público (`/actuator/health`). PLAN-08 lo
  fijó para identity y alineó los planes 02, 03 y 04.
- Tras cada ejecución: detener todos los `java.exe` de los servicios antes de
  empaquetar, o Maven falla al renombrar el jar.

## Historial

| Ciclo | Plan | Resultado | PR | Merge | Notas |
|---|---|---|---|---|---|
| 0 | — | Cerrado | #4 | Sí | Planes versionados e índice §9 |
| 1 | PLAN-00 | Cerrado | #5 | Sí | CA-01..CA-11. Desviación D-01: la colección real tiene 26 peticiones, no 32 |
| 2 | PLAN-01 | Cerrado | #6 | Sí | CA-01..CA-10. `GatewayRoutesController` propio porque Gateway MVC no publica `/actuator/gateway/routes` |
| 3 | PLAN-02 | Cerrado con arreglo | #7 | Sí | CA-01..CA-12. **Bloqueo:** el health de identity devolvía 401 |
| 4 | PLAN-08 | Cerrado | #7 | Sí | Arreglo de una línea + 2 pruebas + alineación de planes 02/03/04. CA-01..CA-13 |
