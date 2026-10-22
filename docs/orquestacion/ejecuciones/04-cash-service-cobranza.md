# Ejecución PLAN-04 + PLAN-09 — cash-service: cobranza y arreglo de ids anidados

Evaluación independiente del orquestador. Dos ciclos en la misma rama y el mismo
PR #9.

## Ciclo A — PLAN-04

### Estado recibido

El primer ejecutor implementó el servicio pero **devolvió la sesión sin informe,
sin push y sin PR**. El orquestador inspeccionó el árbol, verificó el código y
hizo commit + push + PR #9.

### Verificado por el orquestador (13 de 14 criterios)

| CA | Estado | Evidencia |
|---|---|---|
| CA-01 | Cumple | Newman 79 requests / 166 assertions / **0 fallos**, dos corridas consecutivas |
| CA-03 | Cumple | recibo BANCARIO con `cuentaCobrar` (servicio+socio), `banco` {id 4}, `usuario` {id 3, caja2, Lucía, Operadora, OPERADOR} |
| CA-04 | Cumple | `COUNT(*) FROM recibos WHERE usuario_nombres='' OR usuario_apellidos=''` → `0` |
| CA-05 | Cumple | pagos propios: `REC-000219`, secuencia sin huecos |
| CA-06 | Cumple | FKs a `gestion_catalog`/`gestion_auth` → `0` |
| CA-07 | Cumple | catálogo caído: `POST /api/cuentas` → `503`; conteo `351` antes y después |
| CA-08 | Cumple | catálogo caído: `GET /api/cuentas` → `200`; `POST .../pagar` → `201` |
| CA-09 | Cumple | servidor eco en `:9999` registró `GET /api/servicios/1 :: Bearer eyJhbGci...` y `GET /api/puestos/1 :: Bearer ...` — es el JWT del usuario, no credenciales técnicas |
| CA-10 | Cumple | sin token → `401`; operador en `PUT /api/egresos/1/anular` → `403` |
| CA-11 | Cumple | `cash.json`: 13 paths, 14 operaciones, tags `Cuentas,Recibos` |
| CA-12 | Cumple | `Tests run: 7, Failures: 0` |
| CA-13 | Cumple | `egresos -> http://localhost:9090`; `GET /api/egresos?periodo=2026-08` por gateway → `200` |
| CA-14 | Cumple | `{"status":"UP"}` en 9104 sin token |
| **CA-02** | **Fallaba** | `puesto.socio.id` y `puesto.giro.id` en `null` frente al `1` del baseline §1.4 |

### Bloqueo y decisión

CA-02 exigía columnas que el esquema de `gestion_cash` no tiene
(`puesto_socio_id`, `puesto_giro_id`). `V1` es inmutable (ESP-06 §4), así que
cerrarlo requería **una migración nueva**: una decisión de esquema que el plan
original no cubría. Se congeló la evidencia
(`04-cash-service-cobranza-bloqueo.md`) y se encargó el plan de arreglo.

## Ciclo B — PLAN-09 (arreglo)

### Lo que hizo

- `database/flyway/cash/V5__snapshot_ids_puesto.sql`: `ALTER TABLE` con las dos
  columnas `BIGINT NULL` y dos `UPDATE ... INNER JOIN` por igualdad exacta
  (`giros.nombre`, `socios.codigo`), ambos únicos. Sin `LEFT JOIN`, sin
  `COALESCE`, sin FKs, sin índices, `V1`–`V4` intactas.
- `CuentaCobrarEntity`: dos `@Column` sin `@ManyToOne`.
- `CuentaCobrarService.nuevaCuenta`: escribe los ids desde el `PuestoRemoto` que
  ya se obtenía — sin llamadas nuevas a catálogo.
- `CuentaCobrarMapper`: proyecta los ids persistidos y retira el javadoc de la
  desviación.
- 2 pruebas nuevas (9 en total) y 5 asserts nuevos en Postman (171).

### Verificado por el orquestador

| CA | Estado | Evidencia literal |
|---|---|---|
| CA-01 baseline §1.4 | Cumple | JSON de la cuenta `id 255`, `P-101`, `2026-10`: `puesto.socio.id: 1`, `puesto.giro.id: 1`, `socio: null`, `servicio` con sus 5 campos, `lecturaInicial/Final/costoUnitario: null` — idéntico al baseline |
| CA-03 catálogo caído | Cumple | con `catalog-service` detenido: `GET /api/cuentas` → `200` **con los ids** (`socio.id: 1`, `giro.id: 1`), `GET /api/cuentas/puesto/1` → `200` |
| CA-04 alta sin catálogo | Cumple | `POST /api/cuentas` → `503`; cuentas `344 → 344` |
| CA-09 desde cero | Cumple | `DROP DATABASE` + reaplicar el script + arrancar cash: `V1..V5` todas `success=1`, y seeds restaurados 344/216/24 con `RECIBO=216` |
| CA-13 pruebas | Cumple | `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` |
| CA-14 Postman | Cumple | 79 requests / **171 assertions** / 0 fallos |
| CA-12 contrato | Cumple | `git diff --exit-code docs/openapi/cash.json` → exit 0 |
| CA-11 sin FK nuevas | Cumple | única FK sigue siendo `fk_recibos_cuenta` |

Backfill comprobado sobre las 427 filas acumuladas antes del desde-cero:
`304` con puesto, `254` con `puesto_giro_id`, `214` con `puesto_socio_id`,
`50` huérfanas (puestos ya borrados del catálogo) que conservan su snapshot de
texto sin inventar ids, `0` ids inventados.

## Decisiones del orquestador

### D-01 — Autorizar la migración `V5`

La alternativa era dejar CA-02 incumplido o cambiar el baseline publicado. La
migración es la opción que restaura el contrato sin tocarlo, y `V5` es
consecutiva y no colisiona con PLAN-05 (que no crea migraciones; su §4 se ajustó
para admitir `V5` y señalar `V6` como siguiente libre).

### D-02 — Aceptar la lectura de `gestion_catalog` dentro de la migración

Riesgo `Propio-01` del plan. Se acepta porque es **de una sola vez, de solo
lectura, dentro de una migración de datos históricos**, nunca en el camino de
lectura: CA-03 demuestra que listar funciona con catálogo caído y CA-11 que no
quedan FKs externas. No hay escritura cruzada ni dependencia de arranque.

### D-03 — Restaurar los seeds tras verificar

Las corridas de Newman acumulan documentos (ESP-09 §8 lo permite). Antes de
cerrar se dejó `gestion_cash` en el estado de PLAN-00 §9.1 para que los planes
posteriores comparen conteos contra una base conocida.

## Pendiente declarado (no bloquea)

ESP-03 §4.3 y el árbol de ESP-06 §2 no enumeran todavía `puesto_socio_id`,
`puesto_giro_id` ni `V5`. Es coherencia documental, no contradicción: §4.3
enumera mínimos y ESP-04 §5.4 exige esos valores. Se anota para una actualización
futura de especificaciones.
