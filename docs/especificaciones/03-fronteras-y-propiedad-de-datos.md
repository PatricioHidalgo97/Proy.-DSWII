# ESP-03 — Fronteras y propiedad de datos

## 1. Regla fundamental

**Cada dato tiene un único servicio dueño, y solo ese servicio lo escribe.**

Los demás servicios pueden:

- Leerlo por HTTP.
- Guardar una copia local (snapshot) al momento de emitir un documento.

Nunca pueden modificar el dato ajeno.

## 2. Mapa de propiedad

| Entidad / tabla | Servicio dueño | Esquema | ¿Otros la leen? |
|---|---|---|---|
| `usuarios` | identity-service | `gestion_auth` | No (datos en JWT) |
| `socios` | catalog-service | `gestion_catalog` | Sí, caja |
| `giros` | catalog-service | `gestion_catalog` | No directo (viene anidado en puesto) |
| `puestos` | catalog-service | `gestion_catalog` | Sí, caja |
| `servicios` | catalog-service | `gestion_catalog` | Sí, caja |
| `bancos` | catalog-service | `gestion_catalog` | Sí, caja |
| `cuentas_cobrar` | cash-service | `gestion_cash` | No |
| `recibos` | cash-service | `gestion_cash` | No |
| `egresos` | cash-service | `gestion_cash` | No |
| `correlativos` | cash-service | `gestion_cash` | No |

## 3. Claves foráneas permitidas

| Origen | Destino | Permitida |
|---|---|---|
| `puestos.socio_id` | `socios.id` | Sí, misma base |
| `puestos.giro_id` | `giros.id` | Sí, misma base |
| `recibos.cuenta_cobrar_id` | `cuentas_cobrar.id` | Sí, misma base |
| Cualquier tabla de caja | Tabla de catálogo | **No** |
| Cualquier tabla de caja | Tabla de identidad | **No** |
| Cualquier tabla de catálogo | Tabla de caja | **No** |

Caja guarda **identificadores remotos sin clave foránea**. La integridad se
mantiene en la capa de aplicación.

## 4. Qué guarda caja de cada entidad ajena

### 4.1 Usuario (al emitir recibo o egreso)

| Columna local | Origen |
|---|---|
| `usuario_id` | `usuarioId` del JWT |
| `usuario_usuario` | `sub` del JWT |
| `usuario_nombres` | claim `nombres` |
| `usuario_apellidos` | claim `apellidos` |
| `usuario_rol` | claim `rol` |

### 4.2 Socio destinatario de una cuenta

| Columna local | Origen |
|---|---|
| `socio_id` | id del catálogo |
| `socio_codigo`, `socio_nombres`, `socio_apellidos` | datos del socio |

### 4.3 Puesto destinatario de una cuenta

| Columna local | Origen |
|---|---|
| `puesto_id` | id del catálogo |
| `puesto_numero`, `puesto_nombre_inquilino`, `puesto_giro_nombre` | datos del puesto y su giro |
| `puesto_socio_codigo`, `puesto_socio_nombres`, `puesto_socio_apellidos` | socio del puesto, si existe |

### 4.4 Servicio de una cuenta

| Columna local | Origen |
|---|---|
| `servicio_id` | id del catálogo |
| `servicio_nombre`, `servicio_moneda`, `servicio_cargo_a`, `servicio_depende_consumo` | datos del servicio |

### 4.5 Banco de un canje

| Columna local | Origen |
|---|---|
| `banco_id` | id del catálogo |
| `banco_nombre`, `banco_numero_cuenta`, `banco_cci`, `banco_moneda` | datos del banco |

## 5. Por qué snapshots y no consultas en vivo

| Criterio | Snapshot | Consulta en vivo |
|---|---|---|
| Rendimiento al listar | Sin llamadas remotas | N+1 llamadas por fila |
| Documento histórico | Inmutable y fiel al momento de emisión | Cambia si cambia el catálogo |
| Catálogo caído | Se sigue mostrando el histórico | Listados fallan |
| Consistencia | Puede diferir del maestro actual | Siempre actual |
| Complejidad | Mayor tamaño de tabla | Mayor acoplamiento |

Se elige **snapshot** porque los documentos financieros son históricos por
naturaleza: un recibo debe mostrar el nombre y el monto tal como se emitieron.

## 6. Operaciones que cruzan la frontera

| Operación de caja | Llamada a catálogo | Cuándo |
|---|---|---|
| Crear cuenta individual | `GET /api/servicios/{id}` y `GET /api/puestos/{id}` o `GET /api/socios/{id}` | Antes de insertar |
| Generar cuentas masivas por puesto | `GET /api/servicios/{id}` y `GET /api/puestos` | Antes del lote |
| Generar cuentas masivas por socio | `GET /api/servicios/{id}` y `GET /api/socios` | Antes del lote |
| Canje bancario | `GET /api/bancos/{id}` | Antes de emitir el recibo |
| Reporte de socios | `GET /api/socios` | Al generar el XLSX |
| Reporte de bancos | `GET /api/bancos` | Al generar el XLSX |
| Pago individual | Ninguna | — |
| Pago en lote | Ninguna | — |
| Ingreso externo | Ninguna | — |
| Egreso | Ninguna | — |
| Importación CSV de egresos | Ninguna | — |
| Anulación / procesamiento | Ninguna | — |

## 7. Propagación de identidad

Caja llama a catálogo **reenviando el token del usuario**. No usa credenciales
de servicio ni tokens técnicos.

```http
GET /api/servicios/5 HTTP/1.1
Host: localhost:9103
Authorization: Bearer <jwt-del-usuario>
```

Consecuencia: si el usuario no tiene permiso para leer catálogo, la operación
falla con el mismo código que produciría catálogo directamente. Esto mantiene
las reglas de autorización en un único lugar.

## 8. Manejo de fallos de catálogo

| Situación | Comportamiento |
|---|---|
| Catálogo responde 404 | Caja responde 404 con mensaje de recurso no encontrado |
| Catálogo responde 401/403 | Caja propaga el código |
| Catálogo no responde o da timeout | Caja responde **503** con mensaje de dependencia no disponible |
| Falla en medio de un lote | No se insertó nada: la transacción local no se inicia |

Caja no reintenta automáticamente. Un reintento silencioso podría duplicar
documentos; la decisión es fallar explícito.

## 9. Datos que no se replican

| Dato | Motivo |
|---|---|
| Contraseñas | Nunca salen de identidad |
| Fechas de nacimiento de socios | No se usan en documentos financieros |
| Descripción de giros | Solo se usa el nombre en el snapshot del puesto |
| Recurrencia y costo de servicios | El costo se congela en el monto de la cuenta |
| Datos completos de usuarios | Solo se copia lo que el recibo necesita mostrar |

## 10. Consistencia aceptada

Se acepta que el snapshot puede quedar desactualizado respecto al maestro. Por
ejemplo, si un socio cambia de nombre, los recibos ya emitidos conservan el
nombre anterior.

Esta es una **decisión de negocio**, no un defecto: refleja lo que ocurrió al
momento de emitir el documento.
