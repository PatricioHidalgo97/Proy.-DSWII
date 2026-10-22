# ESP-06 — Persistencia, Flyway y seeds

## 1. Decisión

| Aspecto | Decisión |
|---|---|
| Motor | MySQL 8 |
| Bases | Una por microservicio: `gestion_auth`, `gestion_catalog`, `gestion_cash` |
| Creación de esquema | Flyway |
| Datos iniciales | Flyway |
| `ddl-auto` de Hibernate | `validate` |
| Script legado | `database/crear_base.sql` se conserva sin cambios |

## 2. Estructura

```text
database/
├─ crear_base.sql                     # legado del monolito (intacto)
├─ crear_base_microservicios.sql      # crea las tres bases y el usuario
└─ flyway/
   ├─ auth/
   │  ├─ V1__creacion_usuarios.sql
   │  └─ V2__seed_usuarios.sql
   ├─ catalog/
   │  ├─ V1__creacion_catalogos.sql
   │  ├─ V2__seed_giros_socios.sql
   │  ├─ V3__seed_puestos.sql
   │  └─ V4__seed_servicios_bancos.sql
   └─ cash/
      ├─ V1__creacion_tesoreria.sql
      ├─ V2__seed_cuentas_cobrar.sql
      ├─ V3__seed_recibos.sql
      └─ V4__seed_egresos_correlativos.sql
```

## 3. Configuración de Flyway por servicio

```properties
spring.flyway.enabled=true
spring.flyway.locations=filesystem:../../database/flyway/{servicio}
spring.flyway.baseline-on-migrate=false
spring.jpa.hibernate.ddl-auto=validate
```

Ajustar `{servicio}` según el servicio (`auth`, `catalog`, `cash`).

### 3.1 Por qué `filesystem` y no `classpath`

- Centraliza los scripts en `database/`, donde el equipo y el profesor los
  pueden revisar.
- Evita duplicar los mismos SQL dentro de cada `src/main/resources`.

La ruta `filesystem:../../database/flyway/{servicio}` es **relativa al
directorio del servicio**, que es exactamente donde ESP-06 §6.1 ejecuta
`mvn spring-boot:run`:

```text
Proy.-DSWII/                 <- raíz del repositorio
├─ database/flyway/auth/     <- scripts
└─ microservices/
   └─ identity-service/      <- directorio de trabajo de mvn
      └─ ../..  ->  raíz del repositorio
```

Por eso los servicios se arrancan desde su propia carpeta: si se ejecutara
Maven desde la raíz, `../../` apuntaría fuera del repositorio y Flyway no
encontraría los scripts.

### 3.2 Alternativa empaquetada

Si se prefiere que cada servicio sea autónomo, los scripts se copian a
`src/main/resources/db/migration` y se usa la ubicación por defecto
(`classpath:db/migration`). Ambas opciones son válidas; se elige `filesystem`
por trazabilidad.

## 4. Convenciones de migraciones

| Regla | Detalle |
|---|---|
| Nombre | `V{n}__{descripcion_corta}.sql` |
| Separador | Doble guion bajo |
| Numeración | Enteros consecutivos, sin huecos |
| Una migración ya aplicada | Nunca se edita |
| Corrección de datos | Nueva migración |
| Comentario de cabecera | Servicio, esquema y propósito |

## 5. Datos de demostración

### 5.1 Volumen

| Esquema | Tabla | Filas |
|---|---|---|
| auth | `usuarios` | 4 |
| catalog | `socios` | 24 |
| catalog | `giros` | 8 |
| catalog | `puestos` | 26 |
| catalog | `servicios` | 12 |
| catalog | `bancos` | 5 |
| cash | `cuentas_cobrar` | 344 |
| cash | `recibos` | 216 |
| cash | `egresos` | 24 |

### 5.2 Periodos cubiertos

`2026-08`, `2026-09` y `2026-10`.

### 5.3 Distribución de estados

| Estado de cuenta | Cantidad |
|---|---|
| PAGADA | 204 |
| PENDIENTE | 121 |
| EXONERADA | 19 |

### 5.4 Tipos de recibo

| Tipo | Cantidad |
|---|---|
| INGRESO | 190 |
| INGRESO_EXTERNO | 12 |
| BANCARIO | 14 |

### 5.5 Estados de egreso

| Estado | Cantidad |
|---|---|
| PROCESADO | 15 |
| REGISTRADO | 7 |
| ANULADO | 2 |

### 5.6 Casos de negocio incluidos

Los seeds cubren deliberadamente los casos que se quieren demostrar:

- Puestos **sin socio** asignado.
- Puestos con socio y giro.
- Puestos **inactivos** con fecha de fin.
- Socios **inactivos**.
- Servicios fijos y servicios **dependientes de consumo** con lecturas.
- Cuentas por **puesto** y por **socio**.
- Servicios con cargo a puesto y con cargo a socio.
- Cuentas **exoneradas**.
- Pagos con recibo de tipo `INGRESO`.
- **Canjes bancarios simulados** (`BANCARIO`).
- **Ingresos externos** con depositante, categoría y concepto.
- Egresos registrados, procesados y **anulados**.
- Correlativos que continúan la numeración correctamente.

### 5.7 Coherencia garantizada

| Regla | Verificación |
|---|---|
| Toda cuenta tiene puesto o socio, nunca ambos | `puesto_id` XOR `socio_id` |
| Toda cuenta pagada tiene un recibo | No hay cuentas `PAGADA` sin recibo |
| Ningún recibo apunta a una cuenta no pagada | Verificado |
| Un recibo por cuenta | Restricción única |
| Correlativos sin colisiones | `REC-000216` y `EGR-000024` como último número |
| Snapshot completo | Todos los campos de snapshot no nulos según corresponda |
| Socios y giros de puestos existen | Integridad referencial por clave foránea |
| Hashes BCrypt válidos | Verificados contra las contraseñas documentadas |

## 6. Creación inicial en una máquina nueva

### 6.1 Pasos

```powershell
# 1. Crear bases y usuario
mysql -u root -p < database/crear_base_microservicios.sql

# 2. Arrancar cada servicio; Flyway aplica sus migraciones
cd microservices/identity-service && mvn spring-boot:run
cd microservices/catalog-service  && mvn spring-boot:run
cd microservices/cash-service     && mvn spring-boot:run
```

Cada servicio selecciona su esquema por la URL JDBC y aplica solo su carpeta de
migraciones.

### 6.2 Orden de arranque

No hay dependencia estricta de arranque entre servicios. Se recomienda:

1. `identity-service`
2. `catalog-service`
3. `cash-service`
4. `api-gateway`
5. `frontend`

### 6.3 Reinicio limpio

Para volver a sembrar los datos:

```sql
DROP DATABASE gestion_auth;
DROP DATABASE gestion_catalog;
DROP DATABASE gestion_cash;
```

Luego se vuelve a ejecutar `crear_base_microservicios.sql` y se reinician los
servicios.

## 7. Relación entre seeds y presentación

El objetivo es que la aplicación se pueda presentar **sin crear datos a mano**:

| Pantalla | Qué se verá |
|---|---|
| Socios | 24 socios, activos e inactivos, con etapas y acciones |
| Giros | 8 giros comerciales |
| Puestos | 26 puestos, con y sin socio, con giro obligatorio |
| Servicios | 12 servicios, fijos y por consumo, para puesto y socio |
| Bancos | 5 cuentas bancarias referenciales |
| Caja | 344 cuentas de tres periodos, con estados variados |
| Recibos | 216 recibos, incluidos canjes e ingresos externos |
| Egresos | 24 egresos en tres estados |
| Reportes | Datos suficientes para los cinco XLSX |

## 8. Compatibilidad con el monolito

| Elemento | Monolito | Microservicios |
|---|---|---|
| Base | `sistema_gestion` | Tres esquemas separados |
| Creación de esquema | Hibernate `ddl-auto=update` | Flyway |
| Script | `crear_base.sql` | `crear_base_microservicios.sql` + `flyway/` |
| Datos iniciales | `DataInitializer` (solo usuarios y correlativos) | Migraciones |
| Convivencia | Puede seguir ejecutándose | No comparte tablas |

El monolito **no** se modifica ni se elimina. Su script y su configuración
quedan intactos.

## 9. Consideraciones sobre identificadores

- Los seeds insertan identificadores explícitos para que las referencias entre
  tablas sean deterministas.
- Los `AUTO_INCREMENT` continúan desde el máximo insertado.
- En caja, como no hay claves foráneas hacia catálogo, los identificadores
  remotos son simplemente números; su coherencia se valida en la aplicación.

## 10. Checklist de persistencia

- [ ] `crear_base.sql` intacto.
- [ ] `crear_base_microservicios.sql` crea tres bases y el usuario.
- [ ] Un único juego de migraciones por servicio.
- [ ] Migraciones aplican sin error desde cero.
- [ ] `ddl-auto=validate` en los tres servicios.
- [ ] Seeds cubren estados y casos de negocio.
- [ ] Correlativos coherentes con los documentos cargados.
- [ ] Sin contraseñas en texto plano.
- [ ] Reinicio limpio documentado.
