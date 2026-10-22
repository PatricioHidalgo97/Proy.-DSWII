# Microservicios — Sistema de Gestión

Este árbol contiene el API Gateway y los tres servicios de la arquitectura de
microservicios. Cada proyecto es **independiente**: no hay POM agregador, no hay
módulo compartido y no hay servidor de descubrimiento (PR-06, ESP-13 §1.1).

## Servicios

| Servicio | Puerto por defecto | Variable de puerto | Base de datos |
|---|---|---|---|
| `api-gateway` | 9101 | `GATEWAY_PORT` | — |
| `identity-service` | 9102 | `IDENTITY_PORT` | `gestion_auth` |
| `catalog-service` | 9103 | `CATALOG_PORT` | `gestion_catalog` |
| `cash-service` | 9104 | `CASH_PORT` | `gestion_cash` |

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8 o superior accesible en `DB_PORT` (por defecto 3306)
- Bases creadas con `database/crear_base_microservicios.sql`

## Estado 3 — arquitectura final (PLAN-05, ESP-11 §2.4)

El estado que deja PLAN-05 es el **Estado 3 del strangler**: toda la API pública
se sirve desde microservicios y el monolito ya no recibe tráfico del frontend.

```text
Angular 4400 -> Gateway 9101 -> identity 9102  (/api/auth/**)
                            -> catalog  9103  (socios, giros, puestos, servicios, bancos)
                            -> cash     9104  (cuentas, recibos, egresos, reportes)
```

- **Los 52 endpoints del contrato viven en microservicios.** El conteo coincide
  con los OpenAPI exportados en `docs/openapi/`: `identity.json` 2 operaciones,
  `catalog.json` 25 y `cash.json` 25.
- **La ruta `monolito` fue retirada.** El gateway declara diez rutas, todas con
  destino explícito; ninguna apunta al puerto 9090. Un prefijo no listado
  devuelve 404 del gateway (ESP-07 §4.1).
- **El monolito queda como respaldo manual.** Sigue siendo ejecutable en 9090
  con su propia base (`sistema_gestion`) y responde login, pero ya no está
  enrutado ni recibe llamadas del frontend (ESP-11 §9).

### Qué prefijo va a dónde

| Prefijo | Servicio dueño | Destino por defecto |
|---|---|---|
| `/api/auth/**` | `identity-service` | `${IDENTITY_URL:http://localhost:9102}` |
| `/api/socios/**` | `catalog-service` | `${CATALOG_URL:http://localhost:9103}` |
| `/api/giros/**` | `catalog-service` | `${CATALOG_URL:http://localhost:9103}` |
| `/api/puestos/**` | `catalog-service` | `${CATALOG_URL:http://localhost:9103}` |
| `/api/servicios/**` | `catalog-service` | `${CATALOG_URL:http://localhost:9103}` |
| `/api/bancos/**` | `catalog-service` | `${CATALOG_URL:http://localhost:9103}` |
| `/api/cuentas/**` | `cash-service` | `${CASH_URL:http://localhost:9104}` |
| `/api/recibos/**` | `cash-service` | `${CASH_URL:http://localhost:9104}` |
| `/api/egresos/**` | `cash-service` | `${CASH_URL:http://localhost:9104}` |
| `/api/reportes/**` | `cash-service` | `${CASH_URL:http://localhost:9104}` |
| cualquier otro `/api/**` | — | **404 del gateway** |

Los destinos se mueven por variable de entorno, sin tocar código:
`IDENTITY_URL` (PLAN-02), `CATALOG_URL` (PLAN-03) y `CASH_URL` (PLAN-04/05).

### Orden de arranque

1. **MySQL** con `gestion_auth`, `gestion_catalog` y `gestion_cash`.
2. **identity-service** (9102): `cd microservices/identity-service && mvn spring-boot:run`
3. **catalog-service** (9103): `cd microservices/catalog-service && mvn spring-boot:run`
4. **cash-service** (9104): `cd microservices/cash-service && mvn spring-boot:run`
5. **api-gateway** (9101): `cd microservices/api-gateway && mvn spring-boot:run`
6. **Frontend** (4400): `cd frontend && npm start`

Cada servicio se arranca **desde su propia carpeta**: las migraciones Flyway se
leen con una ruta relativa al directorio del servicio
(`filesystem:../../database/flyway/{servicio}`). No hay dependencia estricta de
arranque entre servicios (ESP-06 §6.2); el orden de arriba es el recomendado
para que el health del gateway no marque destinos caídos.

Variables mínimas que deben estar exportadas antes de arrancar los cuatro
servicios:

```bash
export DB_USERNAME=<usuario>
export DB_PASSWORD=<contraseña>
export JWT_SECRET=<secreto de al menos 32 caracteres>   # mismo en los cuatro
export IDENTITY_URL=http://localhost:9102
export CATALOG_URL=http://localhost:9103
export CASH_URL=http://localhost:9104
```

`MONOLITO_URL` **ya no se usa**: la ruta comodín desapareció en PLAN-05 T-07.

Verificación rápida del Estado 3:

```bash
# Los cuatro servicios arriba
curl http://localhost:9101/actuator/health
curl http://localhost:9102/actuator/health
curl http://localhost:9103/actuator/health
curl http://localhost:9104/actuator/health

# Diez rutas, ninguna al 9090
curl http://localhost:9101/actuator/gateway/routes

# Un prefijo desconocido responde 404 del gateway (no 503 ni 500)
curl -i http://localhost:9101/api/inexistente
```

## Cómo arrancar un servicio de dominio

Cada servicio tiene su propio `.env.example`. Las credenciales **no** se
versionan: se exportan en la shell o se copian a un `.env` local (ignorado por
Git).

```bash
export DB_USERNAME=<usuario>
export DB_PASSWORD=<contraseña>

cd microservices/identity-service
mvn spring-boot:run
```

El health check queda disponible en `http://localhost:<puerto>/actuator/health`.

## Configuración

Todos los puertos, URLs y credenciales se leen de variables de entorno con los
valores por defecto de ESP-02 §1. Ningún valor se fija en el código.

| Variable | Servicio | Valor por defecto |
|---|---|---|
| `GATEWAY_PORT` | `api-gateway` | 9101 |
| `IDENTITY_URL` | `api-gateway` | `http://localhost:9102` |
| `CATALOG_URL` | `api-gateway`, `cash-service` | `http://localhost:9103` |
| `CASH_URL` | `api-gateway` | `http://localhost:9104` |
| `CORS_ORIGINS` | `api-gateway` | `http://localhost:4400` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | los tres servicios | `localhost:3306`, sin valor por defecto en credenciales |
| `JWT_SECRET` | los tres servicios | sin valor por defecto: el servicio no arranca si falta |
| `CATALOG_CONNECT_TIMEOUT_MS`, `CATALOG_READ_TIMEOUT_MS` | `cash-service` | 1500 / 3000 |

Las migraciones Flyway viven centralizadas en `database/flyway/{servicio}` y se
leen con una ruta relativa al directorio del servicio:

```properties
spring.flyway.locations=filesystem:../../database/flyway/{servicio}
```

Por eso los servicios se arrancan **desde su propia carpeta**.

## Contratos exportados

Cada servicio expone su OpenAPI en `/v3/api-docs`. Los archivos versionados en
`docs/openapi/` se regeneran desde el servicio en ejecución:

```bash
curl http://localhost:9102/v3/api-docs -o docs/openapi/identity.json
curl http://localhost:9103/v3/api-docs -o docs/openapi/catalog.json
curl http://localhost:9104/v3/api-docs -o docs/openapi/cash.json
```

## Reversa al Estado 2 (respaldo al monolito)

La reversa es de configuración, no de código (ESP-11 §8, PLAN-05 §10):

1. Volver a declarar en `api-gateway/src/main/resources/application.properties`
   una ruta `monolito` con `Path=/api/**`, `uri=${MONOLITO_URL:http://localhost:9090}`
   y un `order` mayor que 33.
2. Exportar `MONOLITO_URL` y reiniciar el gateway.
3. Si se quiere volver al Estado 0, devolver `apiUrl` a
   `http://localhost:9090/api` en `frontend/src/environments/environment.ts` y
   arrancar el monolito.

Mientras la ruta `monolito` no se vuelva a declarar, `MONOLITO_URL` no tiene
efecto: el gateway responde 404 a cualquier prefijo no listado.
