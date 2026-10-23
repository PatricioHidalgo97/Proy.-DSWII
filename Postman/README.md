# Postman — Arquitectura de microservicios

Esta carpeta contiene los artefactos de Postman para probar **todas las rutas** del sistema en su arquitectura de microservicios (Estado 3 del estrangulador, PLAN-05):

```
Angular 4400 -> API Gateway 9101 -> identity-service 9102  (/api/auth/**)
                            -> catalog-service 9103       (/api/socios, /api/giros, /api/puestos,
                                                             /api/servicios, /api/bancos)
                            -> cash-service 9104          (/api/cuentas, /api/recibos, /api/egresos,
                                                             /api/reportes)
```

La colección apunta al **gateway** (`http://localhost:9101/api`). Nunca apunta al monolito (puerto 9090).

> Los archivos que ya estaban en `docs/postman/` son evidencia de la colección anterior (monolito) y se dejan intactos. Esta carpeta es el entregable nuevo para la arquitectura de microservicios.

## Archivos

| Archivo | Descripción |
|---|---|
| `Microservicios.postman_collection.json` | Colección con **93 requests** que cubren las 52 operaciones del contrato (2 identity, 25 catalog, 25 cash) más salud, seguridad, errores y limpieza. |
| `Local - Gateway.postman_environment.json` | Entorno por defecto: `baseUrl = http://localhost:9101/api`. |
| `Local - Directo Identity.postman_environment.json` | Mismas requests, `baseUrl = http://localhost:9102/api` (prueba identity-service sin gateway). |
| `Local - Directo Catalog.postman_environment.json` | `baseUrl = http://localhost:9103/api` (prueba catalog-service sin gateway). |
| `Local - Directo Cash.postman_environment.json` | `baseUrl = http://localhost:9104/api` (prueba cash-service sin gateway). |
| `fixtures/egresos-demo.csv` | Plantilla CSV para la prueba de `POST /api/egresos/importar`. |

## Requisitos previos

1. Bases de datos de microservicios creadas con `database/crear_base_microservicios.sql` (MySQL 8).
2. Cuatro procesos levantados, cada uno en su puerto:
   - `api-gateway` 9101
   - `identity-service` 9102
   - `catalog-service` 9103
   - `cash-service` 9104
3. Usuarios semilla de identidad (ver `database/flyway/auth/V2__seed_usuarios.sql`):
   - `admin / Admin123*` (rol ADMIN)
   - `caja / Caja123*` (rol OPERADOR)

## Cómo usar

1. En Postman: **Importar** → seleccionar `Microservicios.postman_collection.json`.
2. Importar también el entorno deseado (recomendado: `Local - Gateway.postman_environment.json`) y activarlo.
3. Ejecutar la colección completa con el **Collection Runner** en el orden de las carpetas:
   - `00 · Salud y diagnóstico` — health de cada proceso y las 10 rutas del gateway.
   - `01 · Autenticación` — login admin y operador (guardan `{{token}}` y `{{tokenOperador}}`), usuario actual, login inválido.
   - `02 · Catálogos` — CRUD de socios, giros, puestos, servicios y bancos (incluye duplicados 409 y reglas de negocio 400).
   - `03 · Caja — Cuentas` — cuentas individuales y masivas, consultas, exoneración, pagos y canje bancario.
   - `04 · Recibos` — recibos por fecha, por id, ingreso externo.
   - `05 · Egresos` — egreso, importación CSV, procesar y anular (operador 403 / admin 200).
   - `06 · Reportes` — cinco XLSX con aserciones de Content-Type y Content-Disposition.
   - `07 · Seguridad y errores` — 401, 403, 404 (prefijo no enrutado) y 400 de validación.
   - `08 · Limpieza` — elimina los catálogos creados para que la colección sea repetible.

La colección guarda automáticamente en variables de colección los ids creados (`socioId`, `giroId`, `puestoId`, `servicio*Id`, `bancoId`, `cuenta*Id`, `recibo*Id`, `egreso*Id`) y los reutiliza entre requests, por lo que debe ejecutarse en orden.

## Prueba directa de cada servicio (sin gateway)

Selecciona el entorno `Local - Directo <Servicio>` en lugar del de Gateway y ejecuta la colección. Las 25 operaciones de catalog, las 25 de cash y las 2 de identity deberían pasar contra su servicio directo; las rutas que no le pertenecen devolverán 404 de ese servicio, lo cual es el comportamiento esperado y sirve para aislar si una falla está en el servicio o en el enrutado del gateway.

## Qué valida la colección (muestras)

- Login: 200, rol correcto, token JWT guardado.
- Catálogos: 201 al crear, 200 al listar/obtener/actualizar, 409 al duplicar, 204 al eliminar.
- Caja: cuenta creada con `estado = PENDIENTE` y anidamiento (`puesto`, `socio`, `servicio`); generar duplicado 400; exonerar → `EXONERADA`; pagar → 201 con `correlativo` `REC-`; pagar dos veces 400; canjear con `banco` anidado.
- Egresos: 201 con `comprobante` `EGR-`; importar CSV 201; procesar → `PROCESADO`; anular operador 403; anular admin → `ANULADO`.
- Reportes: 200, `Content-Type` XLSX y `Content-Disposition: attachment`.
- Seguridad: sin token 401, token inválido 401, operador sobre catálogos 403, prefijo no enrutado 404 del gateway, payload inválido 400 con `errors`.

## Notas

- El entorno declara solo `baseUrl` y credenciales; los ids de ejecución viven en **variables de colección** (rellenadas por la propia colección), por lo que no se pierden al cambiar de entorno.
- Si Postman no resuelve `fixtures/egresos-demo.csv` en la importación de CSV, selecciona el archivo manualmente desde `Postman/fixtures/`.
- Esta colección no modifica la configuración de CI/CD, ramas ni secretos.
