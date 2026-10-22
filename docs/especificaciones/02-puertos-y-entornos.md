# ESP-02 — Puertos y entornos

## 1. Decisión

Todos los puertos son **configurables por variable de entorno** y tienen un
valor por defecto que **no coincide con ningún puerto ocupado** por los otros
proyectos de la máquina.

| ID | Componente | Variable | Puerto por defecto | URL base |
|---|---|---|---|---|
| PORT-GW | API Gateway | `GATEWAY_PORT` | **9101** | `http://localhost:9101` |
| PORT-AUTH | identity-service | `IDENTITY_PORT` | **9102** | `http://localhost:9102` |
| PORT-CATALOG | catalog-service | `CATALOG_PORT` | **9103** | `http://localhost:9103` |
| PORT-CASH | cash-service | `CASH_PORT` | **9104** | `http://localhost:9104` |
| PORT-WEB | Frontend Angular | `WEB_PORT` | **4400** | `http://localhost:4400` |
| PORT-DB | MySQL | `DB_PORT` | **3306** | `localhost:3306` |
| PORT-MONO | Monolito de respaldo | `SERVER_PORT` | **9090** | `http://localhost:9090` |

## 2. Puertos reservados que NO se usan

Se evitan de forma explícita los siguientes puertos, ya ocupados por otro
proyecto de la máquina:

| Rango / puerto | Ocupado por |
|---|---|
| 8761 | discovery-server del otro proyecto |
| 8080 | api-gateway del otro proyecto |
| 8081 – 8087 | servicios de dominio del otro proyecto |
| 8090 | Actuator del api-gateway del otro proyecto |
| 55432 | PostgreSQL del otro proyecto |
| 29092 | Kafka del otro proyecto |
| 5673, 15673 | RabbitMQ del otro proyecto |
| 1025, 8025 | Mailpit del otro proyecto |
| 4200 | frontend público del otro proyecto |
| 4300 | panel administrativo del otro proyecto |
| 9090 | monolito de este proyecto (respaldo) |
| 3306 | MySQL local (se comparte con esquemas distintos) |

**Regla:** si un puerto por defecto de esta arquitectura llegara a chocar, se
cambia la variable de entorno correspondiente. No se cambia el código.

## 3. Configuración por servicio

### API Gateway

```properties
server.port=${GATEWAY_PORT:9101}

app.routes.identity=${IDENTITY_URL:http://localhost:9102}
app.routes.catalog=${CATALOG_URL:http://localhost:9103}
app.routes.cash=${CASH_URL:http://localhost:9104}

app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:4400}
```

### identity-service

```properties
server.port=${IDENTITY_PORT:9102}
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:${DB_PORT:3306}/gestion_auth?createDatabaseIfNotExist=true&serverTimezone=America/Lima}
app.jwt.secret=${JWT_SECRET:...}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:28800000}
```

### catalog-service

```properties
server.port=${CATALOG_PORT:9103}
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:${DB_PORT:3306}/gestion_catalog?createDatabaseIfNotExist=true&serverTimezone=America/Lima}
app.jwt.secret=${JWT_SECRET:...}
```

### cash-service

```properties
server.port=${CASH_PORT:9104}
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:${DB_PORT:3306}/gestion_cash?createDatabaseIfNotExist=true&serverTimezone=America/Lima}
app.jwt.secret=${JWT_SECRET:...}
app.clients.catalog.url=${CATALOG_URL:http://localhost:9103}
```

## 4. Variables compartidas

| Variable | Obligatoria | Uso |
|---|---|---|
| `JWT_SECRET` | Sí | Debe ser idéntica en los tres servicios |
| `DB_USERNAME` | Sí | Usuario MySQL (`gestion_user`) |
| `DB_PASSWORD` | Sí | Contraseña MySQL |
| `DB_PORT` | No | Puerto de MySQL, por defecto 3306 |
| `CORS_ORIGINS` | No | Orígenes permitidos, separados por coma |
| `GATEWAY_PORT` | No | Puerto del gateway |
| `IDENTITY_PORT` | No | Puerto de identidad |
| `CATALOG_PORT` | No | Puerto de catálogo |
| `CASH_PORT` | No | Puerto de caja |
| `IDENTITY_URL` | No | URL interna de identidad vista desde el gateway |
| `CATALOG_URL` | No | URL interna de catálogo vista por gateway y caja |
| `CASH_URL` | No | URL interna de caja vista desde el gateway |

## 5. Archivos de entorno

Cada servicio incluye `.env.example` documentando sus variables. Ningún archivo
`.env` real se versiona.

```text
microservices/
├─ api-gateway/.env.example
├─ identity-service/.env.example
├─ catalog-service/.env.example
└─ cash-service/.env.example
```

Contenido típico:

```dotenv
GATEWAY_PORT=9101
IDENTITY_URL=http://localhost:9102
CATALOG_URL=http://localhost:9103
CASH_URL=http://localhost:9104
CORS_ORIGINS=http://localhost:4400
JWT_SECRET=cambiar-en-produccion
```

## 6. Configuración del frontend

El frontend lee la URL del gateway desde su archivo de entorno:

```ts
// frontend/src/environments/environment.ts
export const environment = {
  apiUrl: 'http://localhost:9101/api',
};
```

Para cambiar el puerto del gateway sin recompilar:

```powershell
$env:GATEWAY_PORT=9105
# y actualizar apiUrl a http://localhost:9105/api
```

El puerto del servidor de desarrollo Angular también es configurable:

```powershell
cd frontend
npm start -- --port 4400
```

O de forma permanente en `frontend/angular.json`, sección
`architect.serve.options.port = 4400`.

## 7. Matriz de conflicto

| Componente | Puerto | ¿Compatible con el otro proyecto? | ¿Compatible con el monolito? |
|---|---|---|---|
| Gateway | 9101 | Sí | Sí |
| Identidad | 9102 | Sí | Sí |
| Catálogo | 9103 | Sí | Sí |
| Caja | 9104 | Sí | Sí |
| Frontend | 4400 | Sí | Sí |
| MySQL | 3306 | Sí (esquemas distintos) | Sí (esquema distinto) |
| Monolito | 9090 | Sí | — |

## 8. Verificación previa al arranque

Antes de levantar el sistema se comprueba que los puertos estén libres:

```powershell
9101,9102,9103,9104,4400 | ForEach-Object {
  $r = Get-NetTCPConnection -LocalPort $_ -State Listen -ErrorAction SilentlyContinue
  if ($r) { "OCUPADO: $_" } else { "libre: $_" }
}
```

Si alguno está ocupado, se cambia la variable correspondiente. No se edita el
código.
