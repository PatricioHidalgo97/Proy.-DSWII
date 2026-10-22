# Bitácora de orquestación

Estado persistente del agente `orquestador`. Sin este archivo no es posible
retomar un plan a medias entre sesiones.

## Estado actual

- Plan en curso: PLAN-01 — API Gateway y traslado del frontend al gateway
- Rama de trabajo: feature/microservicios-api-gateway
- PR: sin PR
- Punto exacto: T-01 (delegación inicial al ejecutor)
- Siguiente acción: recibir y evaluar la entrega de PLAN-01 (CA-01..CA-10).

## Entorno de ejecución (no está en PATH)

| Herramienta | Ruta / valor |
|---|---|
| JDK 21 | `C:\Program Files\Java\jdk-21.0.11` |
| Maven 3.9.16 | `C:\Users\juan2\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin` |
| MySQL 9.7.1 | `C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe`, `root`/`admin` |
| Monolito | puerto 9090, usuario `admin` / `Admin123*` |
| Puertos | 9101 (gateway) y 4400 (frontend) |

## Historial

| Ciclo | Plan | Resultado | PR | Merge | Notas |
|---|---|---|---|---|---|
| 0 | — | Cerrado | #4 | Sí | Se versionaron los ocho planes y el índice §9 |
| 1 | PLAN-00 | Cerrado | #5 | Sí | CA-01..CA-11 verificados. Desviación D-01: la colección real tiene 26 peticiones, no 32 (registrada, sin impacto en CA-09) |
| 2 | PLAN-01 | En ejecución | — | No | Delegado al ejecutor |
