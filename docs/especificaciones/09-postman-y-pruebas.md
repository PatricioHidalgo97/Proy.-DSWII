# ESP-09 — Postman y pruebas

## 1. Decisión

Se entrega una **colección Postman única** que recorre el flujo completo a
través del gateway, más un **entorno local** con las URLs de cada servicio.

| Artefacto | Ubicación |
|---|---|
| Colección | `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json` |
| Entorno | `docs/postman/Local_Microservicios.postman_environment.json` |
| Plantilla CSV | `docs/plantilla_egresos.csv` (existente) |

## 2. Variables del entorno

| Variable | Valor por defecto | Uso |
|---|---|---|
| `baseUrl` | `http://localhost:9101/api` | Todas las llamadas del flujo |
| `identityUrl` | `http://localhost:9102/api` | Pruebas directas de identidad |
| `catalogUrl` | `http://localhost:9103/api` | Pruebas directas de catálogo |
| `cashUrl` | `http://localhost:9104/api` | Pruebas directas de caja |
| `token` | vacío | Se llena tras el login |
| `tokenOperador` | vacío | Login del operador |
| `socioId` | vacío | Se llena al crear socio |
| `giroId` | vacío | Se llena al crear giro |
| `puestoId` | vacío | Se llena al crear puesto |
| `puestoVacioId` | vacío | Puesto sin socio |
| `servicioPuestoId` | vacío | Servicio fijo de puesto |
| `servicioConsumoId` | vacío | Servicio por consumo |
| `servicioSocioId` | vacío | Servicio de socio |
| `bancoId` | vacío | Se llena al crear banco |
| `cuentaPuestoId` | vacío | Se llena al crear cuenta |
| `cuentaSocioId` | vacío | Se llena al crear cuenta de socio |
| `reciboId` | vacío | Se llena al pagar |
| `egresoId` | vacío | Se llena al crear egreso |
| `today` | fecha actual | Se calcula en pre-request |
| `period` | mes actual | Se calcula en pre-request |

## 3. Estructura de la colección

| Carpeta | Contenido |
|---|---|
| `00 · Salud` | Health del gateway y de los servicios |
| `01 · Autenticación` | Login admin, login operador, usuario actual, login inválido |
| `02 · Catálogos` | CRUD de socios, giros, puestos, servicios y bancos |
| `03 · Caja` | Cuentas individuales, masivas, consultas, exoneración, pagos y canje |
| `04 · Recibos` | Consulta por fecha, por id e ingreso externo |
| `05 · Tesorería` | Egreso, importación CSV, procesar y anular |
| `06 · Reportes` | Cinco XLSX |
| `07 · Seguridad` | 401 sin token, 403 con operador, 404, 400, 409 |
| `08 · Contrato` | Asserts sobre campos anidados |

## 4. Pruebas obligatorias por carpeta

### 4.1 Autenticación

```js
pm.test('Login correcto', () => pm.response.to.have.status(200));
pm.collectionVariables.set('token', pm.response.json().token);
pm.test('Rol ADMIN', () => pm.expect(pm.response.json().rol).to.eql('ADMIN'));
```

### 4.2 Catálogos

- Crear devuelve 201 y guarda el id.
- Listar devuelve un array que contiene el id creado.
- Crear duplicado devuelve 409.
- Actualizar devuelve 200.
- Eliminar devuelve 204.

### 4.3 Caja

- Crear cuenta devuelve 201 con `estado = PENDIENTE`.
- La cuenta devuelve `puesto` y `servicio` anidados no nulos.
- Generar duplicado devuelve 400.
- Exonerar cambia el estado a `EXONERADA`.
- Pagar devuelve 201 con `correlativo` que empieza en `REC-`.
- Pagar dos veces devuelve 400.
- Pagar en lote devuelve un recibo por cuenta.

### 4.4 Recibos

- El recibo pagado trae `cuentaCobrar`, `servicio` y `usuario` anidados.
- El recibo bancario trae `banco` no nulo.
- El ingreso externo trae `depositante`, `categoria` y `concepto`.

### 4.5 Tesorería

- Crear egreso devuelve 201 con `comprobante` que empieza en `EGR-`.
- Importar CSV devuelve 201 con al menos una fila.
- Procesar cambia el estado a `PROCESADO`.
- Anular con operador devuelve 403.
- Anular con admin devuelve 200 y estado `ANULADO`.

### 4.6 Reportes

- Cada descarga devuelve 200.
- `Content-Type` es el de XLSX.
- `Content-Disposition` incluye el nombre del archivo.

### 4.7 Seguridad

- Sin token devuelve 401.
- Token inválido devuelve 401.
- Operador intentando crear socio devuelve 403.
- Operador intentando eliminar devuelve 403.
- Recurso inexistente devuelve 404.
- Payload inválido devuelve 400 con `errors`.

### 4.8 Contrato

```js
const cuenta = pm.response.json()[0];
pm.test('Cuenta conserva anidamiento', () => {
  pm.expect(cuenta).to.have.property('servicio');
  pm.expect(cuenta.servicio).to.have.property('nombre');
  pm.expect(cuenta).to.have.property('lecturaInicial');
  pm.expect(cuenta).to.have.property('costoUnitario');
});
```

## 5. Scripts a nivel de colección

### 5.1 Pre-request

```js
const d = new Date(Date.now() - new Date().getTimezoneOffset() * 60000);
pm.collectionVariables.set('today', d.toISOString().slice(0, 10));
pm.collectionVariables.set('period', d.toISOString().slice(0, 7));
```

### 5.2 Autenticación

```js
const token = pm.collectionVariables.get('token');
if (token) {
  pm.request.headers.upsert({ key: 'Authorization', value: `Bearer ${token}` });
}
```

## 6. Ejecución automatizada

```powershell
npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json `
  -e docs/postman/Local_Microservicios.postman_environment.json `
  --reporters cli,html `
  --reporter-html-export docs/postman/reporte.html
```

### 6.1 Cuándo ejecutarla

- Antes de cerrar cada fase de implementación.
- Antes de una demostración.
- Después de cualquier cambio de contrato.

## 7. Datos de prueba

La colección crea sus propios datos para no depender de los seeds. Los seeds
sirven para la demostración visual; la colección prueba el flujo desde cero.

### 7.1 Convención de nombres

Usar sufijos únicos para evitar conflictos con los seeds:

```text
SOC-PM-01, P-PM-01, Giro PM, Servicio PM, Banco PM
```

## 8. Criterios de aceptación

| Criterio | Condición |
|---|---|
| Cobertura | Todos los endpoints públicos tienen al menos una petición |
| Asserts | Cada petición valida código y al menos un campo |
| Encadenamiento | Los ids se guardan y se reutilizan |
| Aislamiento | Se puede ejecutar varias veces cambiando el sufijo |
| Seguridad | Incluye casos 401 y 403 |
| Contrato | Valida campos anidados |
| Reportes | Valida binarios |

## 9. Checklist de Postman

- [ ] Colección y entorno versionados.
- [ ] `baseUrl` apunta al gateway.
- [ ] Login guarda el token automáticamente.
- [ ] Flujo completo en orden.
- [ ] Casos negativos incluidos.
- [ ] Asserts de contrato anidado.
- [ ] Reportes validados.
- [ ] Ejecutable con Newman.
- [ ] Sin credenciales reales.
