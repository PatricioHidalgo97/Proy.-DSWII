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

## Estado 1 — operación local (PLAN-01, ESP-11 §2.2)

El estado que deja PLAN-01 es el **Estado 1 del strangler**:

```text
Angular 4400 -> Gateway 9101 -> identity 9102  (/api/auth/**)
                            -> Monolito 9090  (todo lo demás)
```

Mientras los servicios de dominio no expongan endpoints, el gateway enruta por
prefijo y los destinos por defecto apuntan al monolito para que el sistema
completo siga funcionando.

### Qué prefijo va a dónde

| Prefijo | Servicio dueño | Destino por defecto en Estado 1 |
|---|---|---|
| `/api/auth/**` | `identity-service` | monolito (9090) |
| `/api/socios/**` | `catalog-service` | monolito (9090) |
| `/api/giros/**` | `catalog-service` | monolito (9090) |
| `/api/puestos/**` | `catalog-service` | monolito (9090) |
| `/api/servicios/**` | `catalog-service` | monolito (9090) |
| `/api/bancos/**` | `catalog-service` | monolito (9090) |
| `/api/cuentas/**` | `cash-service` | monolito (9090) |
| `/api/recibos/**` | `cash-service` | monolito (9090) |
| `/api/egresos/**` | `cash-service` | monolito (9090) |
| `/api/reportes/**` | `cash-service` | monolito (9090) |
| cualquier otro `/api/**` | — | monolito (9090), ruta `monolito` |

Los destinos se mueven por variable de entorno, sin tocar código:
`IDENTITY_URL` (PLAN-02), `CATALOG_URL` (PLAN-03), `CASH_URL` (PLAN-04/05). La
ruta `monolito` se retira en PLAN-05.

### Orden de arranque

1. **Monolito de respaldo** (9090): `cd backend && mvn spring-boot:run`
2. **API Gateway** (9101): `cd microservices/api-gateway && mvn spring-boot:run`
3. **Frontend** (4400): `cd frontend && npm start`

Los servicios de dominio (`identity-service`, `catalog-service`,
`cash-service`) se arrancan cuando su plan los extrae; no hacen falta para el
Estado 1.

Verificación rápida:

```bash
curl http://localhost:9101/actuator/health
curl http://localhost:9101/actuator/gateway/routes
curl -X POST http://localhost:9101/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usuario":"admin","password":"Admin123*"}'
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

## Orden de arranque completo (ESP-06 §6.2)

No hay dependencia estricta de arranque entre servicios. Se recomienda:

1. `identity-service`
2. `catalog-service`
3. `cash-service`
4. `api-gateway`
5. `frontend`

En el Estado 1 el monolito se arranca antes que el gateway, porque es el destino
efectivo de todas las rutas de dominio.

## Configuración

Todos los puertos, URLs y credenciales se leen de variables de entorno con los
valores por defecto de ESP-02 §1. Ningún valor se fija en el código.

| Variable | Servicio | Valor por defecto |
|---|---|---|
| `GATEWAY_PORT` | `api-gateway` | 9101 |
| `IDENTITY_URL` | `api-gateway` | `http://localhost:9090` (Estado 1) |
| `CATALOG_URL` | `api-gateway` | `http://localhost:9090` (Estado 1) |
| `CASH_URL` | `api-gateway` | `http://localhost:9090` (Estado 1) |
| `MONOLITO_URL` | `api-gateway` | `http://localhost:9090` |
| `CORS_ORIGINS` | `api-gateway` | `http://localhost:4400` |

Las migraciones Flyway viven centralizadas en `database/flyway/{servicio}` y se
leen con una ruta relativa al directorio del servicio:

```properties
spring.flyway.locations=filesystem:../../database/flyway/{servicio}
```

Por eso los servicios se arrancan **desde su propia carpeta**.

## Reversa al Estado 0

La reversa es de configuración, no de código (ESP-11 §8):

1. Detener el gateway.
2. Devolver `apiUrl` a `http://localhost:9090/api` en
   `frontend/src/environments/environment.ts`.
3. Arrancar el frontend; vuelve a hablar con el monolito.

Si solo se quiere cambiar el puerto del gateway o el destino del respaldo, basta
con exportar `GATEWAY_PORT` o `MONOLITO_URL` antes de arrancarlo. No se edita
ningún archivo.
