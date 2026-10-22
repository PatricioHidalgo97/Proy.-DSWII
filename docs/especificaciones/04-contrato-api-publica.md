# ESP-04 — Contrato de API pública

## 1. Regla de oro

**El contrato público no cambia.** Los paths, los nombres de campos, los
anidamientos y los códigos de estado son los mismos que ya consume el frontend
Angular actual.

El gateway reproduce exactamente los prefijos que hoy expone el monolito bajo
`/api`.

## 2. Prefijo y versión

- Todas las rutas públicas cuelgan de `/api`.
- No se introduce versionado en la ruta (`/v1`) en esta etapa.
- La versión del contrato se documenta en OpenAPI.

## 3. Propiedad de cada prefijo

| Prefijo | Servicio | Recurso |
|---|---|---|
| `/api/auth/**` | identity-service | Sesión y usuario actual |
| `/api/socios/**` | catalog-service | Socios |
| `/api/giros/**` | catalog-service | Giros |
| `/api/puestos/**` | catalog-service | Puestos |
| `/api/servicios/**` | catalog-service | Servicios |
| `/api/bancos/**` | catalog-service | Bancos |
| `/api/cuentas/**` | cash-service | Cuentas por cobrar y pagos |
| `/api/recibos/**` | cash-service | Recibos e ingresos externos |
| `/api/egresos/**` | cash-service | Egresos |
| `/api/reportes/**` | cash-service | Reportes XLSX |

## 4. Endpoints

### 4.1 Identidad

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Autentica y devuelve JWT |
| GET | `/api/auth/me` | Autenticado | Devuelve el usuario del token |

### 4.2 Catálogo

Cada recurso de catálogo expone el mismo patrón:

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/{recurso}` | Autenticado | Lista todos |
| GET | `/api/{recurso}/{id}` | Autenticado | Obtiene uno |
| POST | `/api/{recurso}` | ADMIN | Crea |
| PUT | `/api/{recurso}/{id}` | ADMIN | Actualiza |
| DELETE | `/api/{recurso}/{id}` | ADMIN | Elimina |

Recursos: `socios`, `giros`, `puestos`, `servicios`, `bancos`.

### 4.3 Caja

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/cuentas` | Autenticado | Lista cuentas |
| GET | `/api/cuentas/puesto/{puestoId}` | Autenticado | Cuentas de un puesto |
| GET | `/api/cuentas/socio/{socioId}` | Autenticado | Cuentas de un socio |
| POST | `/api/cuentas` | Autenticado | Crea cuenta individual |
| POST | `/api/cuentas/generar/puestos` | Autenticado | Generación masiva por puestos |
| POST | `/api/cuentas/generar/socios` | Autenticado | Generación masiva por socios |
| PUT | `/api/cuentas/{id}/exonerar` | Autenticado | Exonera una cuenta |
| PUT | `/api/cuentas/exonerar-lote` | Autenticado | Exonera en lote |
| POST | `/api/cuentas/{id}/pagar` | Autenticado | Paga y emite recibo |
| POST | `/api/cuentas/pagar-lote` | Autenticado | Paga en lote y emite recibos |
| POST | `/api/cuentas/canjear` | Autenticado | Canje bancario simulado |

### 4.4 Recibos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/recibos?fecha=YYYY-MM-DD&tipo=` | Autenticado | Recibos por fecha |
| GET | `/api/recibos/{id}` | Autenticado | Recibo con detalle |
| POST | `/api/recibos/externos` | Autenticado | Ingreso externo |

### 4.5 Egresos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/egresos?periodo=YYYY-MM` | Autenticado | Egresos del mes |
| GET | `/api/egresos/{id}` | Autenticado | Egreso con detalle |
| POST | `/api/egresos` | Autenticado | Registra egreso |
| POST | `/api/egresos/importar` | Autenticado | Importa CSV (multipart) |
| PUT | `/api/egresos/{id}/procesar` | Autenticado | Procesa |
| PUT | `/api/egresos/{id}/anular` | ADMIN | Anula |

### 4.6 Reportes

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/reportes/movimientos-diarios?fecha=` | Autenticado | XLSX del día |
| GET | `/api/reportes/movimientos-mensuales?periodo=` | Autenticado | XLSX del mes |
| GET | `/api/reportes/socios` | Autenticado | XLSX de socios |
| GET | `/api/reportes/egresos?periodo=` | Autenticado | XLSX de egresos |
| GET | `/api/reportes/bancos` | Autenticado | XLSX de bancos |

## 5. Formas de respuesta obligatorias

### 5.1 Login

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

### 5.2 Usuario actual

```json
{
  "id": 1,
  "usuario": "admin",
  "nombres": "Administrador",
  "apellidos": "General",
  "rol": "ADMIN"
}
```

### 5.3 Puesto

Debe seguir incluyendo los objetos anidados `socio` y `giro`, aunque caja no los
consuma directamente:

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

### 5.4 Cuenta por cobrar

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

En caja los objetos anidados se construyen desde los snapshots. Deben conservar
exactamente los nombres de campo anteriores.

### 5.5 Recibo

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

### 5.6 Egreso

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

## 6. Formato de errores

Todas las respuestas de error conservan la estructura actual:

```json
{
  "timestamp": "2026-10-21T10:15:30.123",
  "status": 400,
  "message": "La lectura final no puede ser menor que la lectura inicial"
}
```

Errores de validación:

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

## 7. Reglas de compatibilidad

1. No se renombra ningún campo existente.
2. No se elimina ningún campo existente.
3. Se pueden **agregar** campos nuevos al final; el frontend los ignora.
4. Los campos anidados que el frontend usa (`puesto`, `socio`, `servicio`,
   `banco`, `usuario`, `cuentaCobrar`) siguen presentes con el mismo nombre.
5. Los `null` se mantienen; no se omiten campos.
6. Las fechas conservan formato ISO (`YYYY-MM-DD` y `YYYY-MM-DDTHH:mm:ss`).

## 8. Verificación del contrato

El contrato se valida de tres formas:

1. **OpenAPI** exportado por servicio (ver ESP-08).
2. **Colección Postman** con asserts sobre los campos anidados (ver ESP-09).
3. **Prueba de regresión manual** del flujo completo antes de cerrar cada fase.
