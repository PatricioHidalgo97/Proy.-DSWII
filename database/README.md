# Base de datos

Esta carpeta contiene dos rutas de arranque que **conviven sin pisarse**.

## 1. Monolito (respaldo)

| Archivo | Uso |
|---|---|
| `crear_base.sql` | Crea la base única `sistema_gestion` que usa `backend/` |

Este archivo **no se modifica ni se elimina**. Sirve para arrancar el monolito
original y para reproducir la versión anterior del proyecto.

```powershell
mysql -u root -p < database/crear_base.sql
```

Las tablas del monolito las crea Hibernate con `ddl-auto=update`.

## 2. Microservicios (arquitectura actual)

| Archivo / carpeta | Uso |
|---|---|
| `crear_base_microservicios.sql` | Crea `gestion_auth`, `gestion_catalog`, `gestion_cash` y el usuario |
| `flyway/auth/` | Esquema y datos de `identity-service` |
| `flyway/catalog/` | Esquema y datos de `catalog-service` |
| `flyway/cash/` | Esquema y datos de `cash-service` |

```powershell
mysql -u root -p < database/crear_base_microservicios.sql
```

Después, cada servicio aplica sus migraciones al arrancar. No hay que ejecutar
los archivos de `flyway/` a mano.

## 3. Orden de las migraciones

| Servicio | Base | Migraciones |
|---|---|---|
| `identity-service` | `gestion_auth` | `V1` esquema, `V2` usuarios |
| `catalog-service` | `gestion_catalog` | `V1` esquema, `V2` giros y socios, `V3` puestos, `V4` servicios y bancos |
| `cash-service` | `gestion_cash` | `V1` esquema, `V2` cuentas, `V3` recibos, `V4` egresos y correlativos |

`V1` siempre crea el esquema. Las demás insertan datos de demostración. Una
migración aplicada **no se edita**: cualquier cambio se agrega como `V5`, `V6`,
etc.

## 4. Datos de demostración

| Tabla | Filas |
|---|---|
| `usuarios` | 4 |
| `socios` | 24 |
| `giros` | 8 |
| `puestos` | 26 |
| `servicios` | 12 |
| `bancos` | 5 |
| `cuentas_cobrar` | 344 |
| `recibos` | 216 |
| `egresos` | 24 |

Cubren los periodos `2026-08`, `2026-09` y `2026-10`, con cuentas pagadas,
pendientes y exoneradas; recibos de pago, canjes bancarios simulados e ingresos
externos; y egresos registrados, procesados y anulados.

### Usuarios de demostración

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123*` | ADMIN |
| `caja` | `Caja123*` | OPERADOR |
| `caja2` | `Demo123*` | OPERADOR |
| `caja3` | `Demo123*` | OPERADOR |

Las contraseñas están cifradas con BCrypt en las migraciones.

## 5. Reinicio limpio

```sql
DROP DATABASE IF EXISTS gestion_auth;
DROP DATABASE IF EXISTS gestion_catalog;
DROP DATABASE IF EXISTS gestion_cash;
```

Luego se vuelve a ejecutar `crear_base_microservicios.sql` y se reinician los
servicios.

## 6. Verificación rápida

```sql
SELECT COUNT(*) FROM gestion_auth.usuarios;          -- 4
SELECT COUNT(*) FROM gestion_catalog.socios;         -- 24
SELECT COUNT(*) FROM gestion_catalog.puestos;        -- 26
SELECT COUNT(*) FROM gestion_cash.cuentas_cobrar;    -- 344
SELECT COUNT(*) FROM gestion_cash.recibos;           -- 216
SELECT COUNT(*) FROM gestion_cash.egresos;           -- 24
SELECT * FROM gestion_cash.correlativos;             -- RECIBO 216, EGRESO 24
```

## 7. Documentación relacionada

- [`../docs/especificaciones/06-persistencia-flyway-seeds.md`](../docs/especificaciones/06-persistencia-flyway-seeds.md)
- [`../docs/especificaciones/03-fronteras-y-propiedad-de-datos.md`](../docs/especificaciones/03-fronteras-y-propiedad-de-datos.md)
