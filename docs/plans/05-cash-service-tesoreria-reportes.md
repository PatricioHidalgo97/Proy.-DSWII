# PLAN-05 — cash-service: tesorería, egresos y reportes

## 1. Objetivo

Completar `cash-service` con egresos, importación CSV y los cinco reportes XLSX,
reutilizando la base, los correlativos y el cliente de catálogo que dejó el
PLAN-04. Al cerrar este plan, los 52 endpoints del contrato viven en
microservicios y la ruta `monolito` del gateway se desactiva.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-01 | §3.3, §7 | Cohesión del dinero; egresos y reportes en caja |
| ESP-03 | §1, §4.1, §4.5, §5, §6 | Snapshot de usuario y de banco |
| ESP-04 | §4.5, §4.6, §5.6, §6, §7 | Contrato de egresos y reportes |
| ESP-05 | §5, §11 | Anulación solo `ADMIN` |
| ESP-06 | §1, §2, §3, §5, §7 | `V4` y datos de egresos |
| ESP-08 | §7.3, §8 | Documentación de CSV y de binarios |
| ESP-11 | §3 (F3), §4.3, §7 (Fase 3), §8 | Cierre de la fase 3 |
| ESP-12 | R-01, R-02, R-04, R-12 | Riesgos aplicables |
| ESP-13 | §1.2, §3, §5.1, §6 | Nomenclatura, estilo y terminado |

## 3. Alcance

### Dentro

- `EgresoEntity` con snapshot de usuario.
- `IEgresoService` con sus cinco operaciones, incluida la importación CSV.
- `IReporteService` con los cinco reportes XLSX.
- `EgresoController` y `ReporteController`.
- `Content-Disposition` y tipo MIME de XLSX.
- Documentación OpenAPI de CSV y de binarios.
- Pruebas de transición de estado y de importación.
- Carpetas `05 · Tesorería` y `06 · Reportes` de Postman.
- Retiro de los prefijos `egresos` y `reportes` y desactivación de la ruta `monolito`.
- Actualización de `docs/openapi/cash.json` con los 23 paths y las 25 operaciones totales.

### Fuera

- Cambios en cobranza (PLAN-04).
- Cambios en `docs/plantilla_egresos.csv`.
- Reportes nuevos no contemplados en ESP-04 §4.6.
- Envío de los XLSX por correo o almacenamiento externo.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-04 cerrado | Índice de `docs/plans/README.md` |
| `gestion_cash` con `V1` a `V4` aplicadas, más `V5` si PLAN-09 ya se ejecutó | `flyway_schema_history` |
| 24 egresos sembrados en tres estados | Conteos de PLAN-00 §9.1 |
| Correlativo `EGRESO = 24` | `SELECT ultimo_numero FROM correlativos WHERE tipo='EGRESO'` |
| `docs/plantilla_egresos.csv` presente con dos filas | Lectura del archivo |

PLAN-05 no crea migraciones en `cash`: la `V4` ya trae los seeds de egresos y
del correlativo. Si alguna vez necesitara una, tomará el número siguiente al
máximo aplicado en `flyway_schema_history` (`V6` si la `V5` de PLAN-09 ya
existe), para no colisionar con la numeración consecutiva de ESP-06 §4.

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-05 compara el egreso con ESP-04 §5.6; T-08 verifica binarios y cabeceras |
| R-02 Snapshots incompletos | Alto | T-01 escribe el snapshot del usuario desde `UsuarioPrincipal`; T-06 lo prueba |
| R-04 Secretos distintos | Alto | T-01 reutiliza la seguridad del PLAN-04; T-08 prueba el mismo token |
| R-12 Contratos divergentes | Medio | T-04 regenera `docs/openapi/cash.json` en el mismo plan |

## 6. Tareas

### T-01 — Implementar egresos

- **Objetivo:** las cinco operaciones de ESP-04 §4.5.
- **Especificación:** ESP-04 §4.5, §5.6; ESP-03 §4.1.
- **Archivos:**
  - `.../cash/entities/EgresoEntity.java`
  - `.../cash/enums/EstadoEgreso.java`
  - `.../cash/repositories/EgresoRepository.java`
  - `.../cash/services/{IEgresoService,EgresoService}.java`
  - `.../cash/controllers/EgresoController.java`
  - `.../cash/dto/EgresoRequest.java`
- **Depende de:** PLAN-04.
- **Pasos:**
  1. Crear la entidad con las columnas de la tabla `egresos` y el snapshot de usuario de ESP-03 §4.1.
  2. Copiar las reglas intactas: un anulado no se procesa, un anulado no se anula dos veces, `fechaActualizacion` se actualiza.
  3. Construir el snapshot del usuario desde `UsuarioPrincipal`.
  4. Mantener el comprobante desde `CorrelativoService` con prefijo `EGR`.
  5. Conservar los códigos 201 y 200.
- **Verificación:** crear un egreso devuelve 201 con `comprobante` que empieza en `EGR-`.
- **Terminado cuando:** las cinco operaciones responden igual que el monolito.

### T-02 — Implementar la importación CSV

- **Objetivo:** importar el archivo con el mismo formato y las mismas validaciones.
- **Especificación:** ESP-04 §4.5; ESP-08 §7.3.
- **Archivos:**
  - `.../cash/services/EgresoService.java` (método `importCsv`)
  - `microservices/cash-service/src/main/resources/application.properties` (tamaño de multipart)
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar el parser: separador `;`, salto de la cabecera, mínimo 8 columnas, columna 9 opcional.
  2. Conservar las validaciones de campos en blanco y de montos.
  3. Conservar el error con el número de fila.
  4. Fijar `spring.servlet.multipart.max-file-size=2MB` y `max-request-size=2MB`.
  5. No modificar `docs/plantilla_egresos.csv`.
- **Verificación:** importar la plantilla devuelve 201 con dos egresos y comprobantes consecutivos.
- **Terminado cuando:** la plantilla importa sin error y un CSV inválido devuelve 400 con el número de fila.

### T-03 — Implementar los cinco reportes XLSX

- **Objetivo:** generar los binarios con las mismas hojas y columnas.
- **Especificación:** ESP-04 §4.6; ESP-08 §7.3.
- **Archivos:**
  - `.../cash/services/{IReporteService,ReporteService}.java`
  - `.../cash/controllers/ReporteController.java`
  - `microservices/cash-service/pom.xml` (POI 5.4.1)
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar `ReporteService` con sus cinco métodos y los mismos encabezados.
  2. En `movimientosDiarios` y `movimientosMensuales`, generar las hojas `Ingresos` y `Egresos`.
  3. Sustituir las lecturas de `SocioRepository` y `BancoRepository` por `CatalogoClient`.
  4. En `writeRecibos`, obtener el destinatario desde el snapshot: `socio_nombres` o `puesto_numero`, nunca desde una relación JPA.
  5. Conservar congelado de paneles, autofiltro y autoajuste de columnas.
  6. Devolver `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` y `Content-Disposition` de tipo attachment.
- **Verificación:** los cinco endpoints devuelven 200 con el MIME de XLSX y el nombre de archivo correcto.
- **Terminado cuando:** los cinco XLSX se abren y muestran los datos esperados.

### T-04 — Regenerar el contrato exportado

- **Objetivo:** que `docs/openapi/cash.json` documente los 25 endpoints.
- **Especificación:** ESP-08 §7.3, §8, §10.
- **Archivos:** `docs/openapi/cash.json`
- **Depende de:** T-01, T-02, T-03.
- **Pasos:**
  1. Anotar `EgresoController` con `@Tag("Egresos")` y `ReporteController` con `@Tag("Reportes")`.
  2. Documentar el endpoint multipart con su esquema de archivo.
  3. Documentar los cinco reportes como `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
  4. Exportar de nuevo: `curl http://localhost:9104/v3/api-docs -o docs/openapi/cash.json`.
  5. Verificar que el archivo contiene los 23 paths.
- **Verificación:** el JSON tiene 23 paths, 25 operaciones y las cuatro tags.
- **Terminado cuando:** el contrato completo de caja está exportado.

### T-05 — Alinear el contrato con la línea base

- **Objetivo:** verificar egresos contra ESP-04 §5.6.
- **Especificación:** ESP-04 §5.6, §7, §8.
- **Archivos:** ninguno.
- **Depende de:** T-01.
- **Pasos:**
  1. Comparar un egreso con ESP-04 §5.6 campo por campo, incluido `usuario` con sus cinco campos.
  2. Verificar que `fechaActualizacion` es `null` en un egreso recién creado.
  3. Verificar que un egreso procesado tiene `fechaActualizacion` no nula.
- **Verificación:** las tres comprobaciones coinciden.
- **Terminado cuando:** no hay diferencias con la línea base.

### T-06 — Portar las pruebas de tesorería

- **Objetivo:** cubrir transiciones y validaciones.
- **Especificación:** ESP-13 §5, §5.1.
- **Archivos:** `microservices/cash-service/src/test/java/.../cash/services/{EgresoServiceTest,ReporteServiceTest}.java`
- **Depende de:** T-01, T-03.
- **Pasos:**
  1. Probar que un egreso creado queda en `REGISTRADO`.
  2. Probar que procesar cambia a `PROCESADO` y actualiza la fecha.
  3. Probar que un anulado no se procesa.
  4. Probar que anular dos veces devuelve 400.
  5. Probar que la importación de la plantilla crea dos egresos.
  6. Probar que un CSV con siete columnas devuelve error con el número de fila.
  7. Probar que cada reporte devuelve un arreglo no vacío.
- **Verificación:** las siete pruebas pasan.
- **Terminado cuando:** las transiciones y la importación están cubiertas.

### T-07 — Actualizar el gateway y retirar la ruta al monolito

- **Objetivo:** cerrar la migración del tráfico.
- **Especificación:** ESP-11 §2.4, §7 (Fase 3); ESP-07 §4.1.
- **Archivos:** `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** T-01, T-03.
- **Pasos:**
  1. Excluir `/api/egresos/**` y `/api/reportes/**` de la ruta `monolito`.
  2. **Eliminar la ruta `monolito`** completa.
  3. Verificar que toda ruta no listada devuelve 404 del gateway, tal como exige ESP-07 §4.1.
  4. Confirmar que el monolito sigue arrancando en 9090 aunque no reciba tráfico.
- **Verificación:** `GET /api/inexistente` devuelve 404 del gateway y ninguna pantalla del frontend falla.
- **Terminado cuando:** el frontend no genera ninguna llamada al monolito.

### T-08 — Completar tesorería y reportes en Postman

- **Objetivo:** cubrir ESP-09 §4.5 y §4.6.
- **Especificación:** ESP-09 §3, §4.5, §4.6.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-07.
- **Pasos:**
  1. Crear egreso y verificar 201 con `comprobante` que empieza en `EGR-`.
  2. Importar la plantilla y verificar al menos una fila creada.
  3. Procesar y verificar `PROCESADO`.
  4. Anular con operador y verificar 403; anular con admin y verificar 200 y `ANULADO`.
  5. Descargar los cinco reportes y verificar 200, el MIME de XLSX y `Content-Disposition` con nombre de archivo.
- **Verificación:** las dos carpetas pasan completas en Newman.
- **Terminado cuando:** los once endpoints tienen al menos una petición con assert.

### T-09 — Documentar el Estado 3

- **Objetivo:** dejar escrito el estado final de la arquitectura.
- **Especificación:** ESP-11 §2.4, §7, §8.
- **Archivos:**
  - `microservices/README.md`
  - `docs/plans/README.md` (estado de los planes)
- **Depende de:** T-07.
- **Pasos:**
  1. Documentar el arranque de los cuatro servicios y el frontend.
  2. Declarar que la ruta `monolito` fue retirada y que el monolito queda solo como respaldo.
  3. Registrar que los 52 endpoints están en microservicios.
  4. Actualizar el índice de planes con el estado real.
- **Verificación:** el README describe el estado real del repositorio.
- **Terminado cuando:** el README permite reproducir el Estado 3.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Egresos |
| 2 | T-02, T-03, T-05 | CSV, reportes y contrato, todos sobre T-01 |
| 3 | T-04, T-06 | Contrato exportado y pruebas, requieren los tres grupos |
| 4 | T-07 | Gateway sin ruta al monolito |
| 5 | T-08, T-09 | Postman y documentación del Estado 3 |

T-07 es el punto de no retorno del strangler: una vez retirada la ruta
`monolito`, la reversa exige volver a declararla.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | Los 11 endpoints responden por el gateway | Newman de `05 · Tesorería` y `06 · Reportes` en verde | ESP-04 §4.5, §4.6 |
| CA-02 | El egreso conserva el snapshot de usuario | Comparación con ESP-04 §5.6 | ESP-04 §5.6 |
| CA-03 | El comprobante continúa desde 24 | El primer egreso devuelve `EGR-000025` | ESP-06 §5.7 |
| CA-04 | La plantilla CSV importa dos egresos | `POST /api/egresos/importar` devuelve 201 con dos elementos | ESP-04 §4.5 |
| CA-05 | Un CSV inválido devuelve 400 con el número de fila | Prueba con siete columnas | ESP-04 §6 |
| CA-06 | Los cinco XLSX se descargan con nombre | MIME y `Content-Disposition` correctos | ESP-04 §4.6 |
| CA-07 | Un operador no puede anular | 403 en `PUT /api/egresos/{id}/anular` | ESP-05 §5 |
| CA-08 | El contrato completo está exportado | `docs/openapi/cash.json` con 23 paths y 25 operaciones | ESP-08 §8 |
| CA-09 | La ruta al monolito fue retirada | `GET /api/inexistente` devuelve 404 del gateway | ESP-07 §4.1 |
| CA-10 | El frontend no llama al monolito | Detener el monolito y recorrer las once pantallas | ESP-11 §7 Fase 3 |
| CA-11 | El monolito sigue siendo ejecutable | Arranca en 9090 y responde login | ESP-11 §9 |
| CA-12 | Las pruebas de tesorería pasan | `mvn test` en verde | ESP-13 §5.1 |

## 9. Verificación

```powershell
# JWT_SECRET se toma del entorno local; no se escribe en el plan (contrato de planes, seccion 4.2)
if (-not $env:JWT_SECRET) { throw 'Defina JWT_SECRET antes de arrancar los servicios' }
$token = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token
$operador = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"caja\",\"password\":\"Caja123*\"}' | ConvertFrom-Json).token

# CA-02 y CA-03
curl.exe -s -X POST http://localhost:9101/api/egresos `
  -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{\"tipoDocumento\":\"FACTURA\",\"numeroDocumento\":\"F001-900\",\"proveedor\":\"Proveedor Demo\",\"fecha\":\"2026-10-21\",\"subtotal\":100,\"impuesto\":18,\"total\":118,\"motivo\":\"Prueba\",\"documentoAsociado\":\"OC-900\"}'

# CA-04
curl.exe -s -X POST http://localhost:9101/api/egresos/importar `
  -H "Authorization: Bearer $token" -F "archivo=@docs/plantilla_egresos.csv"

# CA-06
curl.exe -s -D - -o salida.xlsx "http://localhost:9101/api/reportes/socios" `
  -H "Authorization: Bearer $token"

# CA-07
curl.exe -i -X PUT http://localhost:9101/api/egresos/1/anular `
  -H "Authorization: Bearer $operador"

# CA-08
curl.exe http://localhost:9104/v3/api-docs -o docs/openapi/cash.json

# CA-09
curl.exe -i http://localhost:9101/api/inexistente -H "Authorization: Bearer $token"

# CA-12
cd microservices/cash-service ; mvn test
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 a T-02 | Restaurar temporalmente la ruta `monolito` con los prefijos `egresos` y `reportes` |
| T-03 | Los reportes vuelven al monolito; el resto de caja no se ve afectado |
| T-07 | Volver a declarar la ruta `monolito` con `Path=/api/**` y `order` alto |
| T-04 | Regenerar el contrato desde el servicio en el estado anterior |
| Cualquier punto | `DROP DATABASE gestion_cash` y reaplicar; el monolito conserva `sistema_gestion` |

## 11. Entregables

```text
microservices/cash-service/src/main/java/com/cibertec/gestion/cash/
├─ controllers/{EgresoController,ReporteController}.java
├─ dto/EgresoRequest.java
├─ entities/EgresoEntity.java
├─ enums/EstadoEgreso.java
├─ repositories/EgresoRepository.java
└─ services/{IEgresoService,EgresoService,IReporteService,ReporteService}.java
microservices/cash-service/src/test/java/.../{EgresoServiceTest,ReporteServiceTest}.java
docs/openapi/cash.json                       (23 paths, 25 operaciones)
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json (carpetas 05 y 06)
microservices/api-gateway/src/main/resources/application.properties (sin ruta monolito)
microservices/README.md                      (Estado 3)
docs/plans/README.md                         (índice actualizado)
```

## 12. Fuera de alcance

- Cambios en cobranza, recibos o correlativos.
- Nuevos reportes distintos de los cinco de ESP-04 §4.6.
- Envío de XLSX fuera de la respuesta HTTP.
- Retirar el monolito del repositorio.
- Modificación de `database/flyway/cash/**` o de `backend/`.
