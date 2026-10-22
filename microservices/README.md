# Microservicios — Sistema de Gestión

Este árbol contiene los tres servicios de la arquitectura de microservicios.
Cada servicio es un **proyecto Maven independiente**: no hay POM agregador, no
hay módulo compartido y no hay servidor de descubrimiento (PR-06, ESP-13 §1.1).

## Servicios

| Servicio | Puerto por defecto | Variable de puerto | Base de datos |
|---|---|---|---|
| `identity-service` | 9102 | `IDENTITY_PORT` | `gestion_auth` |
| `catalog-service` | 9103 | `CATALOG_PORT` | `gestion_catalog` |
| `cash-service` | 9104 | `CASH_PORT` | `gestion_cash` |

El API Gateway (puerto 9101, `GATEWAY_PORT`) se incorpora en PLAN-01.

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8 o superior accesible en `DB_PORT` (por defecto 3306)
- Bases creadas con `database/crear_base_microservicios.sql`

## Orden de arranque (ESP-06 §6.2)

No hay dependencia estricta de arranque entre servicios. Se recomienda:

1. `identity-service`
2. `catalog-service`
3. `cash-service`
4. `api-gateway` (PLAN-01)
5. `frontend`

## Cómo arrancar un servicio

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

Las migraciones Flyway viven centralizadas en `database/flyway/{servicio}` y se
leen con una ruta relativa al directorio del servicio:

```properties
spring.flyway.locations=filesystem:../../database/flyway/{servicio}
```

Por eso los servicios se arrancan **desde su propia carpeta**.
