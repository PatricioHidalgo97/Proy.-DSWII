# ESP-12 — Riesgos y mitigaciones

## 1. Método

Cada riesgo se describe con **causa**, **impacto**, **probabilidad** y
**mitigación verificable**. La mitigación es parte del plan, no una nota al pie.

| Escala | Significado |
|---|---|
| Probabilidad alta | Puede ocurrir sin aviso |
| Probabilidad media | Ocurre si no se controla |
| Probabilidad baja | Poco probable con el diseño actual |

## 2. Riesgos técnicos

### R-01 — Romper el JSON que consume Angular

| Campo | Detalle |
|---|---|
| Causa | Reemplazar entidades por DTOs y omitir campos anidados |
| Impacto | Alto: pantallas de caja y recibos dejan de funcionar |
| Probabilidad | Alta |
| Mitigación | DTOs explícitos con todos los campos; mapper único; asserts de contrato en Postman; prueba de regresión del flujo completo antes de cerrar la fase |
| Verificación | `GET /api/cuentas` devuelve `puesto`, `socio` y `servicio` anidados |

### R-02 — Snapshots incompletos

| Campo | Detalle |
|---|---|
| Causa | Copiar solo parte de los datos del catálogo |
| Impacto | Alto: documentos con campos nulos o vacíos |
| Probabilidad | Media |
| Mitigación | Contrato de snapshot documentado en ESP-03; columnas `NOT NULL` donde corresponde; validación al insertar |
| Verificación | Ningún recibo o egreso con `usuario_nombres` vacío |

### R-03 — Catálogo caído bloquea altas de caja

| Campo | Detalle |
|---|---|
| Causa | Dependencia síncrona para validar y copiar datos |
| Impacto | Medio: no se pueden crear cuentas ni canjes; lo histórico sigue funcionando |
| Probabilidad | Media |
| Mitigación | Timeout corto; responder 503 con mensaje claro; no insertar nada si falla la lectura; los listados y pagos no dependen de catálogo |
| Verificación | Detener catálogo y comprobar que `GET /api/cuentas` y pagar siguen funcionando |

### R-04 — Secretos JWT distintos

| Campo | Detalle |
|---|---|
| Causa | Copiar configuración sin unificar el secreto |
| Impacto | Alto: tokens válidos rechazados con 401 |
| Probabilidad | Media |
| Mitigación | Un solo `JWT_SECRET` en el entorno; validación al arranque; checklist de seguridad |
| Verificación | Login en identidad y llamada a catálogo con el mismo token |

### R-05 — Correlativos duplicados

| Campo | Detalle |
|---|---|
| Causa | Separar recibos de egresos o alterar el bloqueo pesimista |
| Impacto | Alto: documentos duplicados o saltos de numeración |
| Probabilidad | Baja |
| Mitigación | Mantener recibos, egresos y correlativos en la misma base y servicio; conservar `findByTipoForUpdate`; pruebas de concurrencia básicas |
| Verificación | Dos pagos simultáneos generan correlativos distintos |

### R-06 — CORS mal configurado

| Campo | Detalle |
|---|---|
| Causa | Configurar CORS en servicios además del gateway, o no exponer headers |
| Impacto | Medio: el navegador bloquea peticiones o descargas |
| Probabilidad | Media |
| Mitigación | CORS solo en el gateway; exponer `Content-Disposition`; origen `http://localhost:4400` |
| Verificación | Descarga de XLSX desde el navegador |

### R-07 — Puertos ocupados

| Campo | Detalle |
|---|---|
| Causa | Coincidir con los servicios del otro proyecto de la máquina |
| Impacto | Medio: un servicio no arranca |
| Probabilidad | Baja con el mapa actual |
| Mitigación | Puertos 9101–9104 y 4400, todos configurables; script de verificación previa |
| Verificación | `Get-NetTCPConnection` muestra los puertos libres |

### R-08 — `cash-service` se vuelve un mini-monolito

| Campo | Detalle |
|---|---|
| Causa | Agrupar cobranza, tesorería y reportes en un servicio |
| Impacto | Medio: menos granularidad de la esperada |
| Probabilidad | Alta (es una decisión consciente) |
| Mitigación | Cohesión justificada por el dominio del dinero; punto de corte documentado (separar tesorería); no mezclar catálogo ni identidad |
| Verificación | El servicio no contiene entidades de otros dominios |

### R-09 — Migraciones inconsistentes

| Campo | Detalle |
|---|---|
| Causa | Editar una migración ya aplicada o numerar mal |
| Impacto | Medio: el esquema difiere entre máquinas |
| Probabilidad | Media |
| Mitigación | Convención de nombres; migraciones inmutables; `ddl-auto=validate`; reinicio limpio documentado |
| Verificación | Flyway aplica desde cero sin errores |

### R-10 — Datos de demostración incoherentes

| Campo | Detalle |
|---|---|
| Causa | Seeds con referencias inexistentes o estados imposibles |
| Impacto | Medio: la demostración muestra datos raros |
| Probabilidad | Media |
| Mitigación | Seeds generados con reglas deterministas; verificación de integridad; estados y correlativos coherentes |
| Verificación | Consultas de integridad devuelven cero anomalías |

## 3. Riesgos de proceso

### R-11 — Big bang

| Campo | Detalle |
|---|---|
| Causa | Extraer los tres servicios a la vez |
| Impacto | Alto: no hay versión funcional mientras se migra |
| Probabilidad | Media |
| Mitigación | Patrón strangler por fases; gateway enruta al monolito lo no extraído |
| Verificación | Al final de cada fase el sistema responde |

### R-12 — Divergencia de contratos

| Campo | Detalle |
|---|---|
| Causa | Cambiar el JSON en un servicio sin actualizar la documentación |
| Impacto | Medio: Postman y Swagger mienten |
| Probabilidad | Media |
| Mitigación | OpenAPI exportado en cada cierre de fase; Postman como contrato ejecutable |
| Verificación | La colección pasa completa |

### R-13 — Pérdida del respaldo

| Campo | Detalle |
|---|---|
| Causa | Modificar o eliminar el monolito |
| Impacto | Alto: no hay plan de reversa |
| Probabilidad | Baja |
| Mitigación | `backend/`, `crear_base.sql` y la colección original no se tocan |
| Verificación | El monolito arranca con su perfil demo |

## 4. Riesgos de alcance

### R-14 — Sobrediseño

| Campo | Detalle |
|---|---|
| Causa | Agregar Eureka, Kafka, Docker o sagas “porque sí” |
| Impacto | Medio: más tiempo, más fallos, menos claridad |
| Probabilidad | Media |
| Mitigación | Excluidos explícitamente en ESP-00; solo se agregan si un requisito lo exige |
| Verificación | La arquitectura declarada coincide con la desplegada |

### R-15 — Cambiar el alcance académico

| Campo | Detalle |
|---|---|
| Causa | Implementar pagos reales o integración bancaria |
| Impacto | Alto: incumple el alcance del curso |
| Probabilidad | Baja |
| Mitigación | El módulo bancario sigue siendo simulado y así se documenta |
| Verificación | No hay llamadas a servicios financieros externos |

## 5. Matriz resumida

| ID | Riesgo | Probabilidad | Impacto | Prioridad |
|---|---|---|---|---|
| R-01 | Contrato roto | Alta | Alto | Crítica |
| R-02 | Snapshots incompletos | Media | Alto | Crítica |
| R-04 | Secretos distintos | Media | Alto | Alta |
| R-05 | Correlativos duplicados | Baja | Alto | Alta |
| R-03 | Catálogo caído | Media | Medio | Alta |
| R-06 | CORS | Media | Medio | Media |
| R-07 | Puertos | Baja | Medio | Media |
| R-08 | Mini-monolito | Alta | Medio | Media |
| R-09 | Migraciones | Media | Medio | Media |
| R-10 | Seeds | Media | Medio | Media |
| R-11 | Big bang | Media | Alto | Alta |
| R-12 | Contratos divergentes | Media | Medio | Media |
| R-13 | Pérdida del respaldo | Baja | Alto | Media |
| R-14 | Sobrediseño | Media | Medio | Media |
| R-15 | Alcance académico | Baja | Alto | Media |

## 6. Regla de control

Ninguna fase se cierra si:

1. La colección Postman no pasa.
2. El contrato no está exportado.
3. El monolito no sigue siendo ejecutable.
4. Hay un riesgo crítico sin mitigación aplicada.
