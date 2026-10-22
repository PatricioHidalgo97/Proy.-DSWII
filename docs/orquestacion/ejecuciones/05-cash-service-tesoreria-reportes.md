# Ejecución PLAN-05 — cash-service: tesorería, egresos y reportes

Evaluación independiente del orquestador sobre la rama
`feature/cash-service-tesoreria-reportes` (PR #10).

## Verificación de criterios

| CA | Estado | Evidencia literal |
|---|---|---|
| CA-01 | Cumple | Newman colección completa: **101 requests / 226 assertions / 0 fallos**; `05 · Tesorería` 15/32/0 y `06 · Reportes` 7/23/0 |
| CA-02 | Cumple | `"usuario":{"id":1,"usuario":"admin","nombres":"Administrador","apellidos":"General","rol":"ADMIN"}` |
| CA-03 | Cumple | `comprobante: EGR-000025` (continúa desde 24) |
| CA-04 | Cumple | plantilla → `201` con `EGR-000026,EGR-000027` (2 egresos) |
| CA-05 | Cumple | CSV de 7 columnas → `400` `{"message":"Fila 2: se requieren 8 columnas separadas por punto y coma"}` |
| CA-06 | Cumple | los 5 XLSX: `HTTP 200`, MIME `application/vnd.openxml…spreadsheetml.sheet`, `attachment; filename="…"` y firma ZIP `504b` |
| CA-07 | Cumple | operador → `403` `{"status":403,"message":"No tiene permisos para esta operación"}`; admin → `200` con `estado=ANULADO` |
| CA-08 | Cumple | `cash.json`: `paths 23`, `ops 25`, `tags Cuentas,Egresos,Recibos,Reportes` |
| CA-09 | Cumple | `GET /api/inexistente` → `404` `{"status":404,"error":"Not Found"}`; gateway con **10 rutas**, ninguna a 9090 |
| CA-10 | Cumple | con el monolito **detenido** (9090 → `000`): los 10 prefijos responden `200` y **0 respuestas 5xx** |
| CA-11 | Cumple | `POST http://localhost:9090/api/auth/login` → `200` con token |
| CA-12 | Cumple | `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0` (9 cobranza + 7 egresos + 6 reportes) |

## Verificación del punto de no retorno (T-07)

Rutas activas del gateway tras el plan:

```text
identity -> http://localhost:9102
socios,giros,puestos,servicios,bancos -> http://localhost:9103
cuentas,recibos,egresos,reportes -> http://localhost:9104
```

La ruta `monolito` **no existe**: `grep -c "monolito"` en
`application.properties` devuelve 5 y las 5 son comentarios. `/api/inexistente`
devuelve 404 del gateway, tal como exige ESP-07 §4.1.

## Decisiones aceptadas

### D-01 — Precedencia del destinatario en `writeRecibos`

El ejecutor reports: usa `socio_nombres` si existe, si no `puesto_socio_nombres`,
y si no `"Puesto " + puesto_numero`. ESP-04 §4.6 no fija esa precedencia; el
monolito usaba el socio de la cuenta y luego el número de puesto. Se acepta
como lo más cercano al comportamiento previo sin relación JPA. **Queda anotado
para que ESP-03 §4.2/§4.3 lo declare si se quiere exactitud.**

### D-02 — `CatalogoClient` gana `listBancos()` y `SocioRemoto` gana `accion`

Necesario para los reportes de socios y bancos, que el plan obliga a resolver
por cliente y no por repositorio (ESP-03 §6).

### D-03 — `EgresoResponse` y `UsuarioAnidado` viven en `IEgresoService`

El plan fija la lista de archivos de §11 y no incluye un mapper para egresos.
Se evita crear un archivo fuera de los entregables.

### D-04 — `IDENTITY_URL` del gateway cambió su valor por defecto de 9090 a 9102

Consecuencia directa de retirar la ruta `monolito`: el gateway ya no tiene a
dónde enrutar `/api/auth/**` si no se exporta la variable. Documentado en el
README y en el `.env.example`.

## Observación de verificación

CA-10 se comprobó con el monolito detenido y los 52 endpoints vía curl, no
recorriendo las once pantallas de Angular. El frontend compila contra
`apiUrl: 'http://localhost:9101/api'` y no contiene ninguna referencia a `9090`.
Es evidencia indirecta pero suficiente: si el frontend no tiene la referencia y
el gateway no enruta al monolito, no hay llamada posible.
