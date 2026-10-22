# ESP-10 — Frontend y consumo

## 1. Decisión

El frontend Angular **no se reescribe**. Solo cambia la URL base de la API.

| Elemento | Antes | Después |
|---|---|---|
| `apiUrl` | `http://localhost:9090/api` | `http://localhost:9101/api` |
| Puerto de desarrollo | 4200 (implícito) | 4400 (explícito y configurable) |
| Rutas | Sin cambios | Sin cambios |
| Componentes | Sin cambios | Sin cambios |
| Servicios HTTP | Sin cambios | Sin cambios |
| Interceptor | Sin cambios | Sin cambios |
| Guard | Sin cambios | Sin cambios |

## 2. Por qué 4400

Los puertos 4200 y 4300 están ocupados por los frontends de otro proyecto de la
máquina. El 4400 está libre y es fácil de recordar.

## 3. Cambios exactos

### 3.1 `frontend/src/environments/environment.ts`

```ts
export const environment = {
  apiUrl: 'http://localhost:9101/api',
};
```

### 3.2 `frontend/angular.json`

Agregar el puerto en la sección de `serve`:

```json
"serve": {
  "builder": "@angular/build:dev-server",
  "options": {
    "port": 4400
  },
  "configurations": {
    "production": { "buildTarget": "sistema-gestion:build:production" },
    "development": { "buildTarget": "sistema-gestion:build:development" }
  },
  "defaultConfiguration": "development"
}
```

### 3.3 Nada más

No se tocan:

- `app.routes.ts`
- `app.html`
- `auth.interceptor.ts`
- `auth.guard.ts`
- `auth.service.ts`
- Ningún `*.service.ts`
- Ningún `pages/*`

## 4. Por qué el frontend no nota la diferencia

El gateway reproduce los mismos prefijos y devuelve los mismos JSON. Desde la
perspectiva de Angular:

| Llamada | Antes | Después |
|---|---|---|
| `POST /api/auth/login` | Monolito | Gateway → identidad |
| `GET /api/socios` | Monolito | Gateway → catálogo |
| `GET /api/cuentas` | Monolito | Gateway → caja |
| `POST /api/cuentas/{id}/pagar` | Monolito | Gateway → caja |
| `GET /api/reportes/socios` | Monolito | Gateway → caja → catálogo |

La URL cambia de puerto, no de forma.

## 5. Casos que sí podrían romperse

| Caso | Riesgo | Control |
|---|---|---|
| Campos anidados ausentes | Rompe pantallas de caja y recibos | Asserts de contrato en Postman |
| `usuario` sin `nombres` | Rompe la cabecera | Snapshot completo desde el JWT |
| Fechas con formato distinto | Rompe filtros | Mantener ISO |
| `Content-Disposition` no expuesto | Descarga con nombre incorrecto | Exponer el header en CORS |
| 403 donde antes era 401 | Mensaje de error distinto | Conservar reglas de seguridad |
| Timeout de catálogo en altas | Pantalla muestra error | 503 con mensaje claro |

## 6. Flujo de una pantalla típica

### 6.1 Pantalla de caja

1. Angular pide `GET /api/puestos` → gateway → catálogo.
2. Angular pide `GET /api/servicios` → gateway → catálogo.
3. Angular pide `GET /api/cuentas` → gateway → caja.
4. El usuario crea una cuenta:
   - Angular envía `POST /api/cuentas`.
   - Gateway enruta a caja.
   - Caja consulta servicio y puesto en catálogo.
   - Caja inserta la cuenta con snapshots.
   - Caja devuelve la cuenta con objetos anidados.

La pantalla no distingue cuántos servicios participaron.

## 7. Configuración de desarrollo

```powershell
cd frontend
npm install
npm start -- --port 4400
```

Abrir `http://localhost:4400`.

## 8. Configuración de producción

Para un despliegue real, el frontend debe apuntar a una ruta relativa servida
por un reverse proxy:

```ts
// environment.prod.ts
export const environment = {
  apiUrl: '/api',
};
```

El gateway o el proxy quedan detrás del mismo dominio, eliminando CORS. Esto es
una recomendación de despliegue, no parte del alcance actual.

## 9. Qué no se hace

| Acción | Motivo |
|---|---|
| Convertir el frontend en microfrontends | Fuera de alcance |
| Separar la landing del panel | El proyecto no tiene landing |
| Cambiar a standalone distinto | Ya usa standalone |
| Migrar de Angular 21 | No aporta |
| Reescribir servicios HTTP | El contrato no cambia |
| Agregar manejo de reintentos | Cambiaría el comportamiento |

## 10. Checklist del frontend

- [ ] `apiUrl` apunta al gateway.
- [ ] Puerto de desarrollo 4400.
- [ ] Compila sin errores.
- [ ] Login funciona a través del gateway.
- [ ] Todas las pantallas cargan datos.
- [ ] Descarga de XLSX conserva el nombre.
- [ ] Logout limpia la sesión.
- [ ] Interceptor envía el token.
- [ ] Guard redirige al login sin token.
