# PLAN-00 — Fundaciones, correcciones documentales y línea base del contrato

## 1. Objetivo

Dejar el repositorio preparado para construir los servicios sin escribir lógica
de negocio: estructura de carpetas, POMs, configuración por entorno, corrección
de los defectos documentales detectados y una línea base del contrato público
que sirva de referencia en todos los planes posteriores.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-02 | §1, §3, §4, §5, §8 | Puertos por variable de entorno, `.env.example`, verificación previa |
| ESP-06 | §1, §2, §3, §4, §6, §10 | Flyway por servicio, `ddl-auto=validate`, migraciones inmutables |
| ESP-08 | §1, §2, §3, §8 | springdoc como dependencia de cada servicio, ruta de exportación |
| ESP-09 | §1, §2 | Rutas de la colección y del entorno Postman |
| ESP-11 | §3 (F0), §7 (Fase 0) | Criterios de salida de la fase 0 |
| ESP-12 | R-07, R-09, R-12, R-13, R-14 | Riesgos aplicables |
| ESP-13 | §1.1, §1.5, §2, §6, §9 | Nomenclatura, estructura de servicio, definición de terminado |
| PR-06 | ESP-00 §3 | Sin Eureka, sin mensajería, sin orquestador |

## 3. Alcance

### Dentro

- Creación de `microservices/` con tres esqueletos: `identity-service`,
  `catalog-service`, `cash-service`.
- Un POM independiente por servicio (sin POM agregador, sin módulo compartido).
- Clases de arranque y `application.properties` con puertos y datasource por
  variable de entorno.
- `.env.example` por servicio.
- Verificación de que `database/crear_base_microservicios.sql` y las migraciones
  de `database/flyway/**` aplican desde cero.
- Corrección de la ruta de Flyway en ESP-06 §3.
- `.gitignore` conforme a ESP-13 §9.
- Línea base del contrato público en `docs/contract/baseline-contrato.md`.
- Esqueleto de la colección Postman con las carpetas `00 · Salud` y
  `01 · Autenticación`.
- `microservices/README.md` con el orden de arranque.

### Fuera

- Cualquier endpoint de negocio.
- El API Gateway (PLAN-01).
- Cambios en `backend/`, `database/crear_base.sql` o `frontend/`.
- Migraciones nuevas o edición de las existentes.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| Rama `feature/microservicios-api-gateway` activa | `git branch --show-current` |
| Java 21 y Maven 3.9+ disponibles | `java -version`, `mvn -version` |
| MySQL 8 accesible en el puerto por defecto | `mysql -u root -p -e "SELECT 1"` |
| `database/flyway/{auth,catalog,cash}` con sus 10 scripts | `ls database/flyway/*` |
| `database/crear_base_microservicios.sql` presente | `ls database/crear_base_microservicios.sql` |
| `backend/` compila | `mvn -q -f backend/pom.xml -DskipTests compile` |
| Puertos 9101–9104 y 4400 libres | Script de ESP-02 §8 |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-07 Puertos ocupados | Medio | T-01 verifica los cinco puertos antes de crear nada; todos quedan en variables |
| R-09 Migraciones inconsistentes | Medio | T-06 aplica las migraciones desde cero en bases recién creadas; no se edita ningún script |
| R-12 Contratos divergentes | Medio | T-08 fija la línea base antes de escribir código; T-09 deja la colección lista para crecer |
| R-13 Pérdida del respaldo | Alto | El plan no incluye ninguna tarea que escriba en `backend/` ni en `database/crear_base.sql`; CA-08 lo verifica con `git status` |
| R-14 Sobrediseño | Medio | Tres POMs independientes, sin módulo común, sin agregador, sin descubrimiento |

## 6. Tareas

### T-01 — Verificar disponibilidad de puertos

- **Objetivo:** confirmar que 9101, 9102, 9103, 9104 y 4400 están libres.
- **Especificación:** ESP-02 §8, R-07.
- **Archivos:** ninguno.
- **Depende de:** ninguna.
- **Pasos:**
  1. Ejecutar el script de ESP-02 §8.
  2. Registrar el resultado en el cuerpo del commit de este plan.
  3. Si un puerto está ocupado, definir el valor alterno en el `.env` local, nunca en el código.
- **Verificación:** la salida muestra `libre:` para los cinco puertos.
- **Terminado cuando:** los cinco puertos están libres o tienen un valor alterno documentado.

### T-02 — Crear la estructura de carpetas y los `.env.example`

- **Objetivo:** dejar el árbol de `microservices/` y la configuración de entorno.
- **Especificación:** ESP-13 §2, ESP-02 §5.
- **Archivos:**
  - `microservices/README.md`
  - `microservices/identity-service/.env.example`
  - `microservices/catalog-service/.env.example`
  - `microservices/cash-service/.env.example`
- **Depende de:** T-01.
- **Pasos:**
  1. Crear las tres carpetas de servicio.
  2. Escribir cada `.env.example` con las variables de ESP-02 §4 que aplican.
  3. En `cash-service`, incluir además `CATALOG_URL`.
  4. En `microservices/README.md`, documentar el orden de arranque de ESP-06 §6.2.
- **Verificación:** existen los cuatro archivos y ningún `.env` real está versionado.
- **Terminado cuando:** los cuatro archivos existen y `.env` está ignorado.

### T-03 — Crear los tres POM de servicio

- **Objetivo:** un proyecto Maven independiente por servicio, sin padre común.
- **Especificación:** ESP-13 §1.1, §2; PR-06.
- **Archivos:**
  - `microservices/identity-service/pom.xml`
  - `microservices/catalog-service/pom.xml`
  - `microservices/cash-service/pom.xml`
- **Depende de:** T-02.
- **Pasos:**
  1. Usar `spring-boot-starter-parent` 3.5.16 y `<java.version>21</java.version>`.
  2. Declarar `groupId=com.cibertec`, `artifactId` en kebab-case y `version=1.0.0`.
  3. Incluir `web`, `data-jpa`, `validation`, `actuator`, `flyway-core`, `flyway-mysql`, `mysql-connector-j`, `lombok` y `spring-boot-starter-test`.
  4. No incluir seguridad, JJWT, POI ni springdoc en este plan.
  5. Declarar `spring-boot-maven-plugin` con la exclusión de Lombok.
- **Verificación:** `mvn -q -f microservices/{servicio}/pom.xml -DskipTests package` termina sin error en los tres.
- **Terminado cuando:** los tres proyectos empaquetan.

### T-04 — Crear las clases de arranque y la configuración base

- **Objetivo:** cada servicio arranca, aplica su Flyway y expone su health.
- **Especificación:** ESP-13 §1.1, §2; ESP-02 §3; ESP-06 §3.
- **Archivos:**
  - `microservices/identity-service/src/main/java/com/cibertec/gestion/identity/IdentityServiceApplication.java`
  - `microservices/catalog-service/src/main/java/com/cibertec/gestion/catalog/CatalogServiceApplication.java`
  - `microservices/cash-service/src/main/java/com/cibertec/gestion/cash/CashServiceApplication.java`
  - `src/main/resources/application.properties` en cada servicio
- **Depende de:** T-03.
- **Pasos:**
  1. Crear la clase `{Servicio}Application` con `@SpringBootApplication`.
  2. Declarar `server.port` desde la variable de ESP-02 §1.
  3. Declarar `spring.datasource.url` con la base del servicio y `createDatabaseIfNotExist=true`.
  4. Declarar `spring.datasource.username` y `password` desde `DB_USERNAME` y `DB_PASSWORD`, sin valor por defecto.
  5. Declarar `spring.flyway.enabled=true`, `spring.flyway.locations` y `spring.flyway.baseline-on-migrate=false`.
  6. Declarar `spring.jpa.hibernate.ddl-auto=validate` y `spring.jpa.open-in-view=false`.
  7. Exponer solo `health` en Actuator.
- **Verificación:** `mvn spring-boot:run` en `identity-service` responde `UP` en `/actuator/health`.
- **Terminado cuando:** los tres servicios arrancan y su health responde `UP`.

### T-05 — Corregir la ruta de Flyway en ESP-06

- **Objetivo:** que la ruta documentada apunte a `database/flyway/{servicio}` real.
- **Especificación:** ESP-06 §3, §3.1; ESP-06 §6.1.
- **Archivos:** `docs/especificaciones/06-persistencia-flyway-seeds.md`
- **Depende de:** T-04.
- **Pasos:**
  1. Sustituir `filesystem:../database/flyway/auth` por `filesystem:../../database/flyway/{servicio}`.
  2. Explicar en §3.1 que la ruta es relativa al directorio del servicio, que es donde ESP-06 §6.1 ejecuta `mvn spring-boot:run`.
  3. No cambiar ninguna otra decisión del documento.
- **Verificación:** la ruta del documento coincide con la usada en los `application.properties` de T-04 y con `ls ../../database/flyway/auth` desde la carpeta del servicio.
- **Terminado cuando:** documento y configuración coinciden.

### T-06 — Validar bases y migraciones desde cero

- **Objetivo:** comprobar que los scripts existentes aplican sin error y siembran lo que ESP-06 declara.
- **Especificación:** ESP-06 §1, §2, §5, §6.3, §9.
- **Archivos:** ninguno (los scripts ya existen y no se editan).
- **Depende de:** T-05.
- **Pasos:**
  1. Ejecutar `mysql -u root -p < database/crear_base_microservicios.sql`.
  2. Arrancar los tres servicios y esperar a que Flyway termine.
  3. Ejecutar las consultas de conteo de §9 de este plan.
  4. Ejecutar las consultas de integridad de §9 de este plan.
  5. Si una cuenta no cuadra, **no** editar la migración: registrar el hallazgo y detener el plan.
- **Verificación:** las consultas de §9 devuelven los valores esperados.
- **Terminado cuando:** los nueve conteos y las dos consultas de integridad cuadran.

### T-07 — Crear `.gitignore` conforme a ESP-13

- **Objetivo:** que `.env`, `node_modules`, `target` y `data` dejen de versionarse.
- **Especificación:** ESP-13 §9.
- **Archivos:** `.gitignore`
- **Depende de:** T-03.
- **Pasos:**
  1. Crear `.gitignore` en la raíz con las cuatro reglas de ESP-13 §9.
  2. Agregar `.idea/`, `*.iml`, `dist/` y `*.log`.
  3. Ejecutar `git rm -r --cached frontend/node_modules backend/target backend/data` para dejar de rastrearlos sin borrarlos del disco.
  4. Verificar que los archivos siguen existiendo en el sistema de archivos.
- **Verificación:** `git status --short` no reporta archivos borrados del disco; `git check-ignore` confirma las reglas.
- **Terminado cuando:** `.gitignore` existe y los artefactos ya no están rastreados.

### T-08 — Escribir la línea base del contrato

- **Objetivo:** fijar la forma exacta del JSON que hoy consume Angular, antes de escribir cualquier DTO.
- **Especificación:** ESP-04 §1, §5, §6, §7, §8; ESP-13 §4.
- **Archivos:** `docs/contract/baseline-contrato.md`
- **Depende de:** ninguna.
- **Pasos:**
  1. Transcribir las seis formas obligatorias de ESP-04 §5.1 a §5.6.
  2. Añadir la forma de error de ESP-04 §6 con el ejemplo de validación.
  3. Listar los 52 endpoints de ESP-04 §4 en una tabla con método, ruta y rol.
  4. Registrar el inventario de la colección actual: `docs/Sistema_Gestion.postman_collection.json`, 32 ítems, con el nombre de cada petición.
  5. Declarar en el documento que esta línea base es la referencia de comparación de todos los planes y que no se modifica después.
- **Verificación:** el documento contiene seis formas JSON, la de error, la tabla de 52 endpoints y el inventario de 32 ítems.
- **Terminado cuando:** el documento existe y sus totales cuadran con ESP-04.

### T-09 — Crear el esqueleto de la colección Postman

- **Objetivo:** que la colección exista desde el primer plan y crezca por servicio.
- **Especificación:** ESP-09 §1, §2, §5.
- **Archivos:**
  - `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
  - `docs/postman/Local_Microservicios.postman_environment.json`
- **Depende de:** T-08.
- **Pasos:**
  1. Crear la colección con `schema` v2.1.0 y las variables de ESP-09 §2.
  2. Agregar solo las carpetas `00 · Salud` y `01 · Autenticación`.
  3. En `00 · Salud`, incluir `GET {{baseUrl}}/../../actuator/health` del gateway cuando exista; dejar el ítem marcado como pendiente hasta PLAN-01.
  4. En `01 · Autenticación`, incluir login correcto con el assert que guarda `token`.
  5. Agregar el pre-request de colección que calcula `today` y `period`.
  6. Crear el entorno con `baseUrl` apuntando al gateway.
- **Verificación:** `npx newman run <colección> -e <entorno>` ejecuta la colección sin error de parseo.
- **Terminado cuando:** la colección y el entorno son válidos y contienen dos carpetas.

## 7. Orden de ejecución

| Nivel | Tareas | Pueden ir en paralelo |
|---|---|---|
| 1 | T-01 | — |
| 2 | T-02 | — |
| 3 | T-03, T-08 | Sí, no comparten archivos |
| 4 | T-04, T-07, T-09 | Sí; T-07 requiere T-03 y T-09 requiere T-08 |
| 5 | T-05, T-06 | No: T-06 necesita T-04 (los tres servicios) y T-05 solo requiere T-04 |

T-06 es la última tarea: valida las bases que crean los tres servicios de T-04.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | Existen tres POM independientes y empaquetan | `mvn -q -f <pom> -DskipTests package` en los tres | ESP-13 §1.1 |
| CA-02 | Ningún POM declara un padre distinto de `spring-boot-starter-parent` | `grep -c "<parent>" microservices/*/pom.xml` = 1 en cada uno | PR-06 |
| CA-03 | Los tres servicios arrancan sin lógica de negocio | `/actuator/health` responde `UP` en 9102, 9103 y 9104 | ESP-13 §6 |
| CA-04 | Flyway aplica las 10 migraciones desde cero | `flyway_schema_history` sin filas con `success=0` | ESP-06 §4 |
| CA-05 | Los conteos de seeds coinciden con ESP-06 §5.1 | Consultas de §9 | ESP-06 §5.1 |
| CA-06 | La integridad de caja se cumple | Las dos consultas de integridad de §9 devuelven 0 | ESP-06 §5.7 |
| CA-07 | La ruta de Flyway documentada es ejecutable | `ls ../../database/flyway/auth` desde el servicio | ESP-06 §3 |
| CA-08 | `backend/` y `crear_base.sql` no cambian | `git status --short backend database/crear_base.sql` vacío | ESP-11 §9 |
| CA-09 | Existe la línea base del contrato | `docs/contract/baseline-contrato.md` con 52 endpoints y 6 formas | ESP-04 §4, §5 |
| CA-10 | La colección Postman es válida | `npx newman run` no falla al parsear | ESP-09 §1 |
| CA-11 | Ningún secreto está versionado | `git ls-files | grep -c "\.env$"` = 0 | ESP-13 §9 |

## 9. Verificación

### 9.1 Conteos de seeds

```sql
SELECT 'usuarios'      AS tabla, COUNT(*) AS filas FROM gestion_auth.usuarios
UNION ALL SELECT 'socios',    COUNT(*) FROM gestion_catalog.socios
UNION ALL SELECT 'giros',     COUNT(*) FROM gestion_catalog.giros
UNION ALL SELECT 'puestos',   COUNT(*) FROM gestion_catalog.puestos
UNION ALL SELECT 'servicios', COUNT(*) FROM gestion_catalog.servicios
UNION ALL SELECT 'bancos',    COUNT(*) FROM gestion_catalog.bancos
UNION ALL SELECT 'cuentas',   COUNT(*) FROM gestion_cash.cuentas_cobrar
UNION ALL SELECT 'recibos',   COUNT(*) FROM gestion_cash.recibos
UNION ALL SELECT 'egresos',   COUNT(*) FROM gestion_cash.egresos;
```

Esperado: 4, 24, 8, 26, 12, 5, 344, 216, 24.

### 9.2 Integridad

```sql
-- Debe devolver 0: toda cuenta tiene puesto o socio, nunca ambos
SELECT COUNT(*) FROM gestion_cash.cuentas_cobrar
WHERE (puesto_id IS NULL) = (socio_id IS NULL);

-- Debe devolver 0: toda cuenta pagada tiene recibo
SELECT COUNT(*) FROM gestion_cash.cuentas_cobrar c
WHERE c.estado = 'PAGADA'
  AND NOT EXISTS (SELECT 1 FROM gestion_cash.recibos r WHERE r.cuenta_cobrar_id = c.id);
```

### 9.3 Correlativos

```sql
SELECT tipo, ultimo_numero FROM gestion_cash.correlativos;
```

Esperado: `RECIBO = 216`, `EGRESO = 24`.

### 9.4 Resto de criterios

```powershell
# CA-01
mvn -q -f microservices/identity-service/pom.xml -DskipTests package
mvn -q -f microservices/catalog-service/pom.xml  -DskipTests package
mvn -q -f microservices/cash-service/pom.xml     -DskipTests package

# CA-03 (con cada servicio arrancado)
curl.exe http://localhost:9102/actuator/health
curl.exe http://localhost:9103/actuator/health
curl.exe http://localhost:9104/actuator/health

# CA-08
git status --short backend database/crear_base.sql
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-03 / T-04 | Eliminar `microservices/` completo; no hay estado compartido |
| T-06 | `DROP DATABASE gestion_auth, gestion_catalog, gestion_cash` y reaplicar `crear_base_microservicios.sql`; nunca editar migraciones |
| T-07 | `git checkout -- .gitignore` y `git reset` del índice; los archivos siguen en disco |
| T-08 / T-09 | Eliminar `docs/contract/` y `docs/postman/`; no afectan código |

El monolito nunca se toca, por lo que la reversa siempre deja el sistema
funcionando.

## 11. Entregables

```text
microservices/
├─ README.md
├─ identity-service/{.env.example,pom.xml,src/main/java/.../IdentityServiceApplication.java,src/main/resources/application.properties}
├─ catalog-service/{.env.example,pom.xml,src/main/java/.../CatalogServiceApplication.java,src/main/resources/application.properties}
└─ cash-service/{.env.example,pom.xml,src/main/java/.../CashServiceApplication.java,src/main/resources/application.properties}
.gitignore
docs/
├─ contract/baseline-contrato.md
└─ postman/
   ├─ Sistema_Gestion_Microservicios.postman_collection.json
   └─ Local_Microservicios.postman_environment.json
```

## 12. Fuera de alcance

- Endpoints, controladores, servicios de negocio y DTOs.
- El API Gateway y el cambio de `environment.ts`.
- Seguridad, JWT y springdoc (cada servicio los agrega en su propio plan).
- Migraciones nuevas o modificación de las existentes.
- `backend/`, `database/crear_base.sql` y `frontend/`.
