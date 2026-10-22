# Evidencia de bloqueo — PLAN-04 (cash-service: cobranza)

Evidencia congelada para el planificador de arreglos. Salidas literales de la
evaluación del orquestador, no resúmenes.

## Contexto

- Plan: `docs/plans/04-cash-service-cobranza.md` (PLAN-04).
- Rama: `feature/cash-service-cobranza`, PR **#9** (OPEN, base `main`).
- Base: `8b9397e3`. Commits: `3e9503a4`, `0759acab`, `d7ce944b`.
- Entorno: JDK 21, Maven 3.9.16, MySQL 9.7.1 `root`/`admin`, `JWT_SECRET`
  compartido exportado solo en la shell.

## Criterios verificados y correctos (13 de 14)

| CA | Estado | Evidencia literal |
|---|---|---|
| CA-01 | Cumple | Newman **79 requests / 166 assertions / 0 fallos**, en **dos corridas consecutivas** |
| CA-03 | Cumple | recibo BANCARIO con `cuentaCobrar` (servicio+socio), `banco` {id 4, CCI}, `usuario` {id 3, caja2, Lucía, Operadora, OPERADOR} |
| CA-04 | Cumple | `SELECT COUNT(*) FROM recibos WHERE usuario_nombres='' OR usuario_apellidos=''` → `0` |
| CA-05 | Cumple | secuencia `1..282`, `ultimo_numero = 282`, `huecos = 0`; pagos propios dieron `REC-000219` y siguientes |
| CA-06 | Cumple | FKs hacia `gestion_catalog`/`gestion_auth` → `0`; única FK es `recibos → cuentas_cobrar` (local) |
| CA-07 | Cumple | catálogo caído: `POST /api/cuentas` → `503` `{"status":503,"message":"El catálogo no está disponible; intente más tarde"}`; conteo `351` antes **y** después |
| CA-08 | Cumple | catálogo caído: `GET /api/cuentas` → `200`; `POST /api/cuentas/2/pagar` → `201` |
| CA-09 | Cumple | servidor eco en `:9999` registró: `GET /api/servicios/1 :: Bearer eyJhbGciOiJIUzI1NiJ9...` y `GET /api/puestos/1 :: Bearer eyJhbGciOiJIUzI1NiJ9...` — es el JWT del usuario, no credenciales técnicas |
| CA-10 | Cumple | `GET /api/cuentas` sin token → `401`; operador en `PUT /api/egresos/1/anular` → `403` |
| CA-11 | Cumple | `cash.json`: `paths: 13`, `operaciones: 14`, `tags: Cuentas,Recibos` |
| CA-12 | Cumple | `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| CA-13 | Cumple | ruta `egresos -> http://localhost:9090`; `GET /api/egresos?periodo=2026-08` por gateway → `200` |
| CA-14 | Cumple | `GET http://localhost:9104/actuator/health` → `{"status":"UP"}` |

## Defecto — CA-02: los ids anidados de `puesto` salen en `null`

### Síntoma

`GET /api/cuentas` devuelve:

```json
{"id":1,
 "puesto":{"id":1,"numero":"P-101","nombreInquilino":"Rosa Quispe Mamani",
   "socio":{"id":null,"codigo":"SOC-001","nombres":"Rosa","apellidos":"Quispe Mamani"},
   "giro":{"id":null,"nombre":"Abarrotes y víveres"}},
 "socio":null,
 "servicio":{"id":1,"nombre":"Mantenimiento de puesto","moneda":"PEN","cargoA":"PUESTO","dependeConsumo":false},
 "lecturaInicial":null,"lecturaFinal":null,"costoUnitario":null}
```

El baseline `docs/contract/baseline-contrato.md` §1.4 y ESP-04 §5.4 muestran
`"socio":{"id":1,...}` y `"giro":{"id":1,...}`.

### Causa verificada

Columnas reales de `gestion_cash.cuentas_cobrar` (todas):

```text
id periodo monto estado fecha_creacion puesto_id socio_id servicio_id
lectura_inicial lectura_final costo_unitario
servicio_nombre servicio_moneda servicio_cargo_a servicio_depende_consumo
puesto_numero puesto_nombre_inquilino puesto_giro_nombre
puesto_socio_codigo puesto_socio_nombres puesto_socio_apellidos
socio_codigo socio_nombres socio_apellidos
```

**No existen** `puesto_socio_id` ni `puesto_giro_id`. Migraciones aplicadas:
`V1__creacion_tesoreria.sql`, `V2..V4` (seeds). `V1` es inmutable por ESP-06 §4.

`PuestoRemoto` **sí** trae los ids que faltan, así que el dato está disponible en
el momento de crear la cuenta y solo falta dónde persistirlo:

```java
public record PuestoRemoto(Long id, ..., SocioAnidado socio, GiroAnidado giro)
public record SocioAnidado(Long id, String codigo, ...)
public record GiroAnidado(Long id, String nombre)
```

### Por qué no se puede cerrar dentro del plan actual

PLAN-04 §12 y el contrato §4.3 prohíben crear o editar migraciones dentro del
plan. La solución exige una **migración nueva** `V5` en `database/flyway/cash/`,
lo que es una decisión de esquema: por eso se eleva.

### Dato relevante para decidir

`socios.codigo` tiene índice **único** (`uk_socios_codigo`, `NON_UNIQUE=0`), y
`servicios`/`bancos` ya usan `id` propio en sus snapshots. Alternativas posibles:

| Opción | Qué implica |
|---|---|
| `V5` con `puesto_socio_id BIGINT NULL` y `puesto_giro_id BIGINT NULL` | Cierra CA-02 exactamente como pide el baseline. Backfill de las 427 filas existentes posible por `puesto_socio_codigo` → `socios.codigo` (único) y por `puesto_giro_nombre` → `giros.nombre` |
| Ajustar el baseline a `id: null` en esos dos anidados | Cambia el contrato publicado; requiere decisión humana y actualizar ESP-04 §5.4 |
| Resolver al leer contra catálogo | Rompe ESP-03 §5 y §8 (el histórico debe verse con catálogo caído) y T-09 exige listados sin llamadas remotas |

## Observación secundaria (no bloquea)

La colección Postman acumula documentos en `gestion_cash` en cada corrida
(quedaron 427 cuentas y 282 recibos, correlativo 282). No es un fallo: ESP-09 §8
permite reejecutar cambiando el sufijo y ESP-03 §3/§5 dice que los snapshots son
históricos. Pero deja el seed del PLAN-00 §9.1 desviado si alguien compara
conteos después. Nota para el cierre de PLAN-06/07.

## Lo que NO hace falta tocar

- Los otros 13 criterios: todos verificados.
- Contrato de recibos, snapshots de usuario/banco/servicio: correctos.
- Correlativos: secuenciales sin huecos.
- Degradación por catálogo caído: correcta.
- `backend/`, `database/crear_base.sql`, migraciones existentes: intactos.
