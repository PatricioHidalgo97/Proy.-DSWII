# Línea base del contrato público

**Referencia inmutable.** Este documento fija la forma exacta del JSON que hoy
consume el frontend Angular, antes de que exista un solo DTO en los
microservicios. Todos los planes posteriores (PLAN-01 a PLAN-07) se comparan
contra este documento. **No se modifica después**: si el contrato evoluciona,
se documenta en ESP-04 y se refleja en OpenAPI y Postman, pero esta línea base
se conserva como punto de comparación.

- Origen de las formas: ESP-04 §5.1 a §5.6 y §6.
- Origen de la tabla de endpoints: ESP-04 §4.
- Origen del inventario de la colección: `docs/Sistema_Gestion.postman_collection.json`.

---

## 1. Formas de respuesta obligatorias

### 1.1 Login — `POST /api/auth/login`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": 1,
  "usuario": "admin",
  "nombres": "Administrador",
  "apellidos": "General",
  "rol": "ADMIN"
}
```

Campos: `token`, `id`, `usuario`, `nombres`, `apellidos`, `rol`. Ninguno es
nulo. `rol` toma valores `ADMIN` u `OPERADOR`.

### 1.2 Usuario actual — `GET /api/auth/me`

```json
{
  "id": 1,
  "usuario": "admin",
  "nombres": "Administrador",
  "apellidos": "General",
  "rol": "ADMIN"
}
```

Misma forma que el login **sin** el campo `token`.

### 1.3 Puesto — `/api/puestos/**`

Debe incluir los objetos anidados `socio` y `giro` aunque caja no los consuma
directamente:

```json
{
  "id": 1,
  "numero": "P-101",
  "nombreInquilino": "Rosa Quispe Mamani",
  "fechaInicio": "2024-01-01",
  "fechaFin": null,
  "socio": {
    "id": 1,
    "codigo": "SOC-001",
    "nombres": "Rosa",
    "apellidos": "Quispe Mamani",
    "accion": "A-001",
    "etapa": 1,
    "fechaNacimiento": "1978-03-12",
    "activo": true
  },
  "giro": {
    "id": 1,
    "nombre": "Abarrotes y víveres",
    "descripcion": "Venta de productos de primera necesidad",
    "activo": true
  },
  "activo": true
}
```

`fechaFin` puede ser `null` y el campo se mantiene explícito. `socio` puede ser
`null` en un puesto sin socio asignado.

### 1.4 Cuenta por cobrar — `/api/cuentas/**`

```json
{
  "id": 10,
  "periodo": "2026-10",
  "monto": 80.00,
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-10-05T09:10:00",
  "puesto": {
    "id": 1,
    "numero": "P-101",
    "nombreInquilino": "Rosa Quispe Mamani",
    "socio": {
      "id": 1,
      "codigo": "SOC-001",
      "nombres": "Rosa",
      "apellidos": "Quispe Mamani"
    },
    "giro": {
      "id": 1,
      "nombre": "Abarrotes y víveres"
    }
  },
  "socio": null,
  "servicio": {
    "id": 1,
    "nombre": "Mantenimiento de puesto",
    "moneda": "PEN",
    "cargoA": "PUESTO",
    "dependeConsumo": false
  },
  "lecturaInicial": null,
  "lecturaFinal": null,
  "costoUnitario": null
}
```

En caja los objetos anidados se construyen desde los snapshots y conservan
exactamente estos nombres de campo. `puesto` y `socio` son mutuamente
excluyentes: exactamente uno de los dos es no nulo. `lecturaInicial`,
`lecturaFinal` y `costoUnitario` existen siempre, con `null` cuando el servicio
no depende de consumo.

### 1.5 Recibo — `/api/recibos/**`

```json
{
  "id": 1,
  "correlativo": "REC-000001",
  "fecha": "2026-08-06T10:36:00",
  "monto": 50.00,
  "tipo": "BANCARIO",
  "cuentaCobrar": {
    "id": 96,
    "periodo": "2026-08",
    "monto": 50.00,
    "estado": "PAGADA",
    "servicio": { "id": 6, "nombre": "Cuota ordinaria de socio" },
    "socio": { "id": 1, "codigo": "SOC-001", "nombres": "Rosa", "apellidos": "Quispe Mamani" },
    "puesto": null
  },
  "banco": {
    "id": 2,
    "nombre": "BBVA Perú",
    "numeroCuenta": "0011-0455-0100234567",
    "cci": "01104550010023456718",
    "moneda": "PEN",
    "activo": true
  },
  "depositante": null,
  "categoria": null,
  "concepto": "Canje bancario académico",
  "fechaDeposito": "2026-08-06",
  "usuario": {
    "id": 3,
    "usuario": "caja2",
    "nombres": "Lucía",
    "apellidos": "Operadora",
    "rol": "OPERADOR"
  }
}
```

`tipo` toma valores `INGRESO`, `INGRESO_EXTERNO`, `BANCARIO`. `banco` es no
nulo solo en `BANCARIO`; `depositante`, `categoria` y `fechaDeposito` son no
nulos solo en `INGRESO_EXTERNO`.

### 1.6 Egreso — `/api/egresos/**`

```json
{
  "id": 1,
  "comprobante": "EGR-000001",
  "tipoDocumento": "FACTURA",
  "numeroDocumento": "F001-1001",
  "proveedor": "Distribuidora Lima Norte S.A.C.",
  "fecha": "2026-08-02",
  "subtotal": 100.00,
  "impuesto": 18.00,
  "total": 118.00,
  "motivo": "Útiles de limpieza para el mercado",
  "documentoAsociado": "OC-001",
  "estado": "PROCESADO",
  "usuario": {
    "id": 2,
    "usuario": "caja",
    "nombres": "Operador",
    "apellidos": "Caja",
    "rol": "OPERADOR"
  },
  "fechaRegistro": "2026-08-02T09:07:00",
  "fechaActualizacion": "2026-08-02T11:07:00"
}
```

`estado` toma valores `REGISTRADO`, `PROCESADO`, `ANULADO`.

---

## 2. Forma de error

Toda respuesta de error conserva esta estructura:

```json
{
  "timestamp": "2026-10-21T10:15:30.123",
  "status": 400,
  "message": "La lectura final no puede ser menor que la lectura inicial"
}
```

Errores de validación (Bean Validation):

```json
{
  "timestamp": "2026-10-21T10:15:30.123",
  "status": 400,
  "message": "Revise los datos enviados",
  "errors": {
    "codigo": "El código es obligatorio",
    "etapa": "La etapa es obligatoria"
  }
}
```

`errors` solo aparece en errores de validación; es un mapa `campo → mensaje`.

### 2.1 Códigos de estado

| Código | Significado |
|---|---|
| 200 | Operación exitosa |
| 201 | Recurso creado |
| 204 | Eliminación exitosa |
| 400 | Validación o regla de negocio |
| 401 | Sin token o token inválido |
| 403 | Rol insuficiente |
| 404 | Recurso no encontrado |
| 409 | Duplicado o conflicto de integridad |
| 503 | Dependencia (catálogo) no disponible |

---

## 3. Los 52 endpoints públicos

Todos cuelgan de `/api`. No hay versionado en la ruta.

### 3.1 Identidad — identity-service (2)

| # | Método | Ruta | Rol |
|---|---|---|---|
| 1 | POST | `/api/auth/login` | Público |
| 2 | GET | `/api/auth/me` | Autenticado |

### 3.2 Catálogo — catalog-service (25)

Cada uno de los cinco recursos (`socios`, `giros`, `puestos`, `servicios`,
`bancos`) expone el mismo patrón de cinco operaciones.

| # | Método | Ruta | Rol |
|---|---|---|---|
| 3 | GET | `/api/socios` | Autenticado |
| 4 | GET | `/api/socios/{id}` | Autenticado |
| 5 | POST | `/api/socios` | ADMIN |
| 6 | PUT | `/api/socios/{id}` | ADMIN |
| 7 | DELETE | `/api/socios/{id}` | ADMIN |
| 8 | GET | `/api/giros` | Autenticado |
| 9 | GET | `/api/giros/{id}` | Autenticado |
| 10 | POST | `/api/giros` | ADMIN |
| 11 | PUT | `/api/giros/{id}` | ADMIN |
| 12 | DELETE | `/api/giros/{id}` | ADMIN |
| 13 | GET | `/api/puestos` | Autenticado |
| 14 | GET | `/api/puestos/{id}` | Autenticado |
| 15 | POST | `/api/puestos` | ADMIN |
| 16 | PUT | `/api/puestos/{id}` | ADMIN |
| 17 | DELETE | `/api/puestos/{id}` | ADMIN |
| 18 | GET | `/api/servicios` | Autenticado |
| 19 | GET | `/api/servicios/{id}` | Autenticado |
| 20 | POST | `/api/servicios` | ADMIN |
| 21 | PUT | `/api/servicios/{id}` | ADMIN |
| 22 | DELETE | `/api/servicios/{id}` | ADMIN |
| 23 | GET | `/api/bancos` | Autenticado |
| 24 | GET | `/api/bancos/{id}` | Autenticado |
| 25 | POST | `/api/bancos` | ADMIN |
| 26 | PUT | `/api/bancos/{id}` | ADMIN |
| 27 | DELETE | `/api/bancos/{id}` | ADMIN |

### 3.3 Cuentas por cobrar — cash-service (11)

| # | Método | Ruta | Rol |
|---|---|---|---|
| 28 | GET | `/api/cuentas` | Autenticado |
| 29 | GET | `/api/cuentas/puesto/{puestoId}` | Autenticado |
| 30 | GET | `/api/cuentas/socio/{socioId}` | Autenticado |
| 31 | POST | `/api/cuentas` | Autenticado |
| 32 | POST | `/api/cuentas/generar/puestos` | Autenticado |
| 33 | POST | `/api/cuentas/generar/socios` | Autenticado |
| 34 | PUT | `/api/cuentas/{id}/exonerar` | Autenticado |
| 35 | PUT | `/api/cuentas/exonerar-lote` | Autenticado |
| 36 | POST | `/api/cuentas/{id}/pagar` | Autenticado |
| 37 | POST | `/api/cuentas/pagar-lote` | Autenticado |
| 38 | POST | `/api/cuentas/canjear` | Autenticado |

### 3.4 Recibos — cash-service (3)

| # | Método | Ruta | Rol |
|---|---|---|---|
| 39 | GET | `/api/recibos?fecha=YYYY-MM-DD&tipo=` | Autenticado |
| 40 | GET | `/api/recibos/{id}` | Autenticado |
| 41 | POST | `/api/recibos/externos` | Autenticado |

### 3.5 Egresos — cash-service (6)

| # | Método | Ruta | Rol |
|---|---|---|---|
| 42 | GET | `/api/egresos?periodo=YYYY-MM` | Autenticado |
| 43 | GET | `/api/egresos/{id}` | Autenticado |
| 44 | POST | `/api/egresos` | Autenticado |
| 45 | POST | `/api/egresos/importar` | Autenticado |
| 46 | PUT | `/api/egresos/{id}/procesar` | Autenticado |
| 47 | PUT | `/api/egresos/{id}/anular` | ADMIN |

### 3.6 Reportes — cash-service (5)

| # | Método | Ruta | Rol |
|---|---|---|---|
| 48 | GET | `/api/reportes/movimientos-diarios?fecha=` | Autenticado |
| 49 | GET | `/api/reportes/movimientos-mensuales?periodo=` | Autenticado |
| 50 | GET | `/api/reportes/socios` | Autenticado |
| 51 | GET | `/api/reportes/egresos?periodo=` | Autenticado |
| 52 | GET | `/api/reportes/bancos` | Autenticado |

**Totales:** 2 + 25 + 11 + 3 + 6 + 5 = **52**.

---

## 4. Inventario de la colección actual

Archivo: `docs/Sistema_Gestion.postman_collection.json`.

Inventario verificado del archivo versionado. La colección tiene 5 carpetas y
**26 peticiones**.

| # | Carpeta | Petición | Método |
|---|---|---|---|
| 1 | 1. Autenticación | Login administrador | POST |
| 2 | 2. Catálogos | Crear socio | POST |
| 3 | 2. Catálogos | Listar socios | GET |
| 4 | 2. Catálogos | Crear giro | POST |
| 5 | 2. Catálogos | Crear puesto | POST |
| 6 | 2. Catálogos | Crear banco | POST |
| 7 | 2. Catálogos | Listar bancos | GET |
| 8 | 3. Servicios y cuentas | Crear servicio para puesto | POST |
| 9 | 3. Servicios y cuentas | Crear servicio para socio | POST |
| 10 | 3. Servicios y cuentas | Generar cuenta de puesto | POST |
| 11 | 3. Servicios y cuentas | Generar cuenta de socio | POST |
| 12 | 3. Servicios y cuentas | Generar cuentas masivas de puestos | POST |
| 13 | 3. Servicios y cuentas | Consultar por socio | GET |
| 14 | 3. Servicios y cuentas | Pagar cuenta de puesto | POST |
| 15 | 3. Servicios y cuentas | Pagar lote | POST |
| 16 | 4. Tesorería | Canjear cuenta de socio (simulado) | POST |
| 17 | 4. Tesorería | Registrar ingreso externo | POST |
| 18 | 4. Tesorería | Consultar recibos | GET |
| 19 | 4. Tesorería | Registrar egreso | POST |
| 20 | 4. Tesorería | Procesar egreso | PUT |
| 21 | 4. Tesorería | Listar egresos del mes | GET |
| 22 | 5. Reportes XLSX | Movimientos diarios | GET |
| 23 | 5. Reportes XLSX | Movimientos mensuales | GET |
| 24 | 5. Reportes XLSX | Padrón de socios | GET |
| 25 | 5. Reportes XLSX | Reporte de egresos | GET |
| 26 | 5. Reportes XLSX | Reporte de bancos | GET |

Carpetas (5): `1. Autenticación`, `2. Catálogos`, `3. Servicios y cuentas`,
`4. Tesorería`, `5. Reportes XLSX`.

> **Desviación registrada.** El PLAN-00 §T-08 paso 4 declara «32 ítems». El
> archivo real contiene 26 peticiones y 5 carpetas (31 elementos en total). No
> se inventaron elementos para llegar a 32: se transcribe el inventario real.
> La diferencia se reporta al orquestador como decisión pendiente.

---

## 5. Reglas de compatibilidad

1. No se renombra ningún campo existente.
2. No se elimina ningún campo existente.
3. Se pueden **agregar** campos nuevos al final; el frontend los ignora.
4. Los campos anidados que el frontend usa (`puesto`, `socio`, `servicio`,
   `banco`, `usuario`, `cuentaCobrar`) siguen presentes con el mismo nombre.
5. Los `null` se mantienen; no se omiten campos.
6. Las fechas conservan formato ISO (`YYYY-MM-DD` y `YYYY-MM-DDTHH:mm:ss`).

---

## 6. Uso de esta línea base

| Plan | Cómo la usa |
|---|---|
| PLAN-01 | El gateway reproduce los prefijos de la §3 sin alterarlos |
| PLAN-02 a PLAN-05 | Cada DTO se compara contra la forma correspondiente de la §1 |
| PLAN-06 | OpenAPI consolidado se valida contra la §3 |
| PLAN-07 | La colección completa valida los campos anidados de la §1 |

Cualquier discrepancia entre esta línea base y una respuesta real es un
**defecto del servicio**, no una actualización de la línea base.
