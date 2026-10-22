# PLAN-06 — Contrato consolidado, Swagger agregado y verificación de no regresión

## 1. Objetivo

Demostrar que el contrato público no cambió: consolidar los tres OpenAPI
exportados, comparar cada path contra la línea base, y ofrecer un Swagger único
en el gateway para revisar los 52 endpoints desde un solo lugar.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-08 | §1, §6, §8, §10, §11 | Exportación, URLs y relación con Postman |
| ESP-07 | §9, §11 | Swagger agregado opcional y checklist |
| ESP-04 | §3, §4, §7, §8 | Contrato completo y reglas de compatibilidad |
| ESP-13 | §4, §6 | Reglas de contrato y definición de terminado |
| ESP-12 | R-01, R-12 | Riesgos de contrato |
| ESP-11 | §7 (Fase 4) | Criterios de salida |

## 3. Alcance

### Dentro

- Los tres archivos de `docs/openapi/` regenerados y consistentes.
- `docs/contract/verificacion-contrato.md` con la comparación contra la línea base.
- Swagger agregado en el gateway, con un selector por servicio.
- Rutas de documentación en el gateway, sin exponer nada adicional.
- Verificación de que ningún path ni campo desapareció.

### Fuera

- Endpoints nuevos.
- Cambios en DTOs o controladores.
- Publicar Swagger agregado fuera de `localhost`.
- Reemplazar el Swagger por servicio.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-02 a PLAN-05 cerrados | Índice de `docs/plans/README.md` |
| Los tres servicios arrancan | Health de 9102, 9103 y 9104 |
| `docs/openapi/{identity,catalog,cash}.json` existen | `ls docs/openapi` |
| `docs/contract/baseline-contrato.md` existe | Archivo del PLAN-00 |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-02 compara path por path y campo por campo; cualquier diferencia detiene el plan |
| R-12 Contratos divergentes | Medio | T-01 regenera los tres archivos en el mismo commit que documenta la verificación |

## 6. Tareas

### T-01 — Regenerar los tres contratos

- **Objetivo:** partir de la versión desplegada, no de una copia vieja.
- **Especificación:** ESP-08 §8.1, §8.2.
- **Archivos:**
  - `docs/openapi/identity.json`
  - `docs/openapi/catalog.json`
  - `docs/openapi/cash.json`
- **Depende de:** PLAN-05.
- **Pasos:**
  1. Arrancar los tres servicios.
  2. Ejecutar los tres `curl ... /v3/api-docs -o docs/openapi/{servicio}.json`.
  3. Verificar que cada archivo es JSON válido.
  4. Contar los paths de cada archivo y compararlos con §9.1.
- **Verificación:** los tres archivos son válidos y los conteos coinciden.
- **Terminado cuando:** los tres contratos reflejan el código desplegado.

### T-02 — Comparar contra la línea base

- **Objetivo:** demostrar que no se perdió ni se renombró nada.
- **Especificación:** ESP-04 §7; ESP-13 §4.
- **Archivos:** `docs/contract/verificacion-contrato.md`
- **Depende de:** T-01.
- **Pasos:**
  1. Tabular los 52 endpoints de la línea base con una columna de estado: presente, ausente o movido.
  2. Recorrer las seis formas de ESP-04 §5 y marcar cada campo: presente con el mismo nombre, renombrado o ausente.
  3. Registrar el esquema de error y los ocho códigos de ESP-04 §6.
  4. Declarar el resultado con una sola frase: "el contrato es compatible" o "hay N diferencias".
  5. Si hay diferencias, detener el plan y abrir la corrección en el plan del servicio correspondiente.
- **Verificación:** la tabla tiene 52 filas y la frase final es "el contrato es compatible".
- **Terminado cuando:** la verificación está escrita y sin diferencias.

### T-03 — Exponer el Swagger agregado

- **Objetivo:** un solo punto para revisar los tres contratos.
- **Especificación:** ESP-07 §9.2.
- **Archivos:**
  - `microservices/api-gateway/pom.xml`
  - `microservices/api-gateway/src/main/java/com/cibertec/gestion/gateway/config/OpenApiAggregatorConfig.java`
  - `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** T-01.
- **Pasos:**
  1. Agregar springdoc 2.8.14 al gateway.
  2. Declarar las tres rutas de documentación de ESP-07 §9.2 con su `RewritePath`.
  3. Configurar `springdoc.swagger-ui.urls[0..2]` con los nombres Identidad, Catálogo y Caja.
  4. Verificar que el gateway no requiere token para `/swagger-ui/**` ni `/v3/api-docs/**`.
  5. No publicar Actuator ni Swagger fuera de `localhost`.
- **Verificación:** `http://localhost:9101/swagger-ui.html` muestra el selector con tres opciones y las tres cargan.
- **Terminado cuando:** el Swagger agregado funciona sin alterar el de cada servicio.

### T-04 — Documentar el cierre de contrato

- **Objetivo:** dejar constancia de cómo se verifica el contrato en el futuro.
- **Especificación:** ESP-08 §8.2, §10; ESP-13 §4.
- **Archivos:** `docs/contract/README.md`
- **Depende de:** T-02.
- **Pasos:**
  1. Describir los cuatro artefactos: Swagger por servicio, Swagger agregado, OpenAPI exportado y colección Postman.
  2. Explicar cuándo se regenera cada uno.
  3. Enlazar la verificación de contrato.
- **Verificación:** un lector puede regenerar y comparar el contrato sin ayuda.
- **Terminado cuando:** el documento describe el procedimiento completo.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Exportar desde el código desplegado |
| 2 | T-02, T-03 | Comparación y Swagger agregado son independientes |
| 3 | T-04 | Documenta el procedimiento, requiere la comparación cerrada |

T-02 es un punto de control: si encuentra una diferencia de contrato, el plan
se bloquea y la corrección vuelve al servicio correspondiente.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | Los tres contratos son JSON válidos | Parseo sin error | ESP-08 §8 |
| CA-02 | Las operaciones suman 52 y los paths 35 | Conteo de §9.1 | ESP-04 §4 |
| CA-03 | Ningún endpoint de la línea base falta | Tabla de 52 filas sin "ausente" | ESP-04 §7 |
| CA-04 | Ningún campo obligatorio falta o cambió de nombre | Tabla de las seis formas | ESP-04 §7 |
| CA-05 | Los ocho códigos de error están documentados | Revisión del JSON | ESP-04 §6 |
| CA-06 | El Swagger agregado muestra tres servicios | Selector en 9101 | ESP-07 §9.2 |
| CA-07 | El Swagger por servicio sigue funcionando | Las tres UIs responden | ESP-08 §6 |
| CA-08 | Nada de Swagger se publica fuera de localhost | Sin binding a `0.0.0.0` | ESP-07 §7 |
| CA-09 | El procedimiento de verificación está documentado | `docs/contract/README.md` | ESP-13 §4 |

## 9. Verificación

### 9.1 Conteo de paths y operaciones

```powershell
$identity = (Get-Content docs/openapi/identity.json -Raw | ConvertFrom-Json).paths.PSObject.Properties.Name.Count
$catalog  = (Get-Content docs/openapi/catalog.json  -Raw | ConvertFrom-Json).paths.PSObject.Properties.Name.Count
$cash     = (Get-Content docs/openapi/cash.json     -Raw | ConvertFrom-Json).paths.PSObject.Properties.Name.Count
"identity=$identity catalog=$catalog cash=$cash total=$($identity + $catalog + $cash)"
```

Esperado: `identity=2`, `catalog=10`, `cash=23`, `total=35` paths, y 52 operaciones
en total (2 + 25 + 25).

### 9.2 Resto de criterios

```powershell
# CA-06
Start-Process "http://localhost:9101/swagger-ui.html"

# CA-07
curl.exe -s -o $null -w "%{http_code}`n" http://localhost:9102/swagger-ui.html
curl.exe -s -o $null -w "%{http_code}`n" http://localhost:9103/swagger-ui.html
curl.exe -s -o $null -w "%{http_code}`n" http://localhost:9104/swagger-ui.html

# CA-01
Get-Content docs/openapi/*.json -Raw | ConvertFrom-Json | Out-Null
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 | Volver a exportar desde el servicio en el estado anterior |
| T-03 | Quitar la dependencia springdoc del gateway y las tres rutas de documentación |
| T-02 | Ninguna: si hay diferencias, el plan queda bloqueado hasta corregir el servicio |

## 11. Entregables

```text
docs/openapi/{identity,catalog,cash}.json     (regenerados)
docs/contract/verificacion-contrato.md
docs/contract/README.md
microservices/api-gateway/src/main/java/.../config/OpenApiAggregatorConfig.java
microservices/api-gateway/src/main/resources/application.properties (rutas de documentación)
```

## 12. Fuera de alcance

- Endpoints, DTOs o controladores.
- Versionado de la API en la ruta.
- Publicar Swagger en red.
- Reemplazar el Swagger por servicio.
- Colección Postman (PLAN-07).
