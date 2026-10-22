# PLAN-09 — Arreglo de los identificadores anidados de `puesto` en la cuenta por cobrar

## 1. Objetivo

Dejar `GET /api/cuentas` devolviendo `puesto.socio.id` y `puesto.giro.id` con el
valor que publica `docs/contract/baseline-contrato.md` §1.4 y ESP-04 §5.4, tanto
en las cuentas ya emitidas como en las nuevas, sin consultar catálogo al leer y
sin cambiar la forma del JSON. Al cerrar el plan, el CA-02 de PLAN-04 queda
cumplido y verificable.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-03 | §1, §3, §4.3, §5, §6, §7, §8, §9, §10 | El snapshot es la única fuente al leer; los ids remotos son números sin clave foránea; los listados no cruzan la frontera; el histórico se muestra con catálogo caído |
| ESP-04 | §5.4, §7 (reglas 1 a 5), §8 | Forma obligatoria de la cuenta por cobrar: `puesto.socio.id` y `puesto.giro.id` no nulos; sin renombrar ni omitir campos |
| ESP-06 | §1, §2, §3, §4, §5.7, §9 | `gestion_cash` propia, migraciones inmutables, `V{n}__{descripcion}.sql` consecutiva, `ddl-auto=validate`, ids remotos como números |
| ESP-12 | R-01, R-02, R-03, R-09, R-10, R-12 | Riesgos aplicables con su mitigación en este plan (§5) |
| ESP-13 | §1.3, §3, §5, §5.1, §6 | Nombre de columnas e índices, estilo, pruebas por regla, health como condición de terminado |
| ESP-09 | §4.3, §4.8, §8 | Asserts de contrato sobre los anidados en la colección ejecutable |
| ESP-08 | §8 | El contrato exportado sigue siendo válido y no cambia |
| ESP-02 | §1, §4 | Solo se citan los puertos por defecto; no se fija ninguno nuevo en código |
| `docs/contract/baseline-contrato.md` | §1.4 | Oráculo literal del valor esperado (`"socio":{"id":1,...}`, `"giro":{"id":1,...}`) |

## 3. Alcance

### Dentro

- Una migración nueva `V5` en `database/flyway/cash/` que añade `puesto_socio_id`
  y `puesto_giro_id` a `cuentas_cobrar` y reconstruye esos dos ids en las filas
  ya emitidas.
- El mapeo JPA de las dos columnas en `CuentaCobrarEntity`.
- La escritura de los dos ids en el alta de cuentas, desde `PuestoRemoto`.
- La proyección de los dos ids en `CuentaCobrarMapper` y el retiro de los
  javadoc que documentaban la desviación.
- Pruebas de servicio y asserts de Postman que impiden la regresión.
- Alineación de la precondición de migraciones de PLAN-05.

### Trazabilidad con lo que se repara

| Elemento de origen | Ubicación | Qué hace este plan |
|---|---|---|
| CA-02 de PLAN-04 («Una cuenta conserva sus tres anidamientos») | `04-cash-service-cobranza.md` §8 | Es el criterio que se rompió; lo re-verifica T-09 |
| T-09 de PLAN-04 («Alinear el contrato con la línea base») | `04-cash-service-cobranza.md` §6 | Se re-ejecuta con el oráculo del baseline, paso 1 de T-09 |
| Restricción «no se modifica `database/flyway/cash/**`» | `04-cash-service-cobranza.md` §12 | Se levanta **solo** para este plan: la migración es la decisión que se eleva y se autoriza |
| Commit `d7ce944b` («documentar la desviación de CA-02») | rama `feature/cash-service-cobranza` | Su texto se corrige en T-05: la desviación deja de existir |
| `V1__creacion_tesoreria.sql` | `database/flyway/cash/` | **No se edita** (ESP-06 §4); se extiende con `V5` |

### Fuera

- Cambiar el baseline, ESP-04 §5.4 o el nombre de cualquier campo del JSON.
- Resolver ids consultando catálogo en lectura o listado.
- Egresos, reportes, correlativos y recibos (PLAN-05).
- La observación secundaria de la evidencia (427 cuentas acumuladas por
  reejecuciones de Postman): es comportamiento permitido por ESP-09 §8 y se
  atiende en el cierre de PLAN-06/07.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-04 ejecutado sobre la rama `feature/cash-service-cobranza` (PR #9 abierto) con CA-01 y CA-03 a CA-14 verificados | Informe de `docs/orquestacion/ejecuciones/04-cash-service-cobranza-bloqueo.md` |
| `gestion_cash` con `V1` a `V4` aplicadas y `success = 1` | `SELECT version, success FROM gestion_cash.flyway_schema_history ORDER BY installed_rank` devuelve 1, 2, 3, 4 |
| **No existe todavía una `V5` en `database/flyway/cash/`** | `ls database/flyway/cash/` lista exactamente `V1` a `V4` |
| `gestion_catalog` **sembrado** (`V2` a `V4`), no solo creado | `SELECT COUNT(*) FROM gestion_catalog.giros` = 8, `socios` = 24, `puestos` = 26 |
| El usuario de Flyway puede leer `gestion_catalog` | `SHOW GRANTS FOR CURRENT_USER()` incluye `gestion_catalog.*`, como concede `database/crear_base_microservicios.sql` |
| La cuenta del baseline existe y es única | `SELECT COUNT(*) FROM gestion_cash.cuentas_cobrar WHERE puesto_id = 1 AND periodo = '2026-10' AND servicio_id = 1` = 1 |
| `identity-service`, `catalog-service`, `cash-service` y el gateway arrancan | `GET /actuator/health` en 9102, 9103 y 9104 devuelve `{"status":"UP"}` y el gateway enruta `/api/**` |
| `JWT_SECRET` exportado en la shell y compartido por los tres servicios | La variable existe y no está vacía; el login por el gateway devuelve `token` |
| JDK 21 y Maven 3.9.x disponibles | `mvn -v` informa Java 21 |

> **Sobre la precondición de `gestion_catalog` sembrado.** La `V5` reconstruye
> los ids leyendo `gestion_catalog.giros` y `gestion_catalog.socios` **una sola
> vez, durante la migración**, en lugar de consultar catálogo al leer. No exige
> `catalog-service` en ejecución —el servicio puede estar apagado—, así que no
> crea una dependencia de arranque entre servicios (ESP-06 §6.2): exige el
> **esquema sembrado**, que en el arranque limpio de ESP-06 §6.1 ya ocurrió
> antes de caja. La alternativa (consultar catálogo en cada lectura) está
> prohibida por ESP-03 §5 y §8; la alternativa de no rellenar deja el defecto
> vivo en las 344 cuentas sembradas y en la cuenta del baseline.

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | El JSON no cambia de forma: solo dos valores pasan de `null` al id publicado. T-09 compara los once campos de ESP-04 §5.4 contra el baseline §1.4 y T-07 añade asserts de los dos ids en Postman |
| R-02 Snapshots incompletos | Alto | T-04 escribe los ids en el alta desde `PuestoRemoto`, que ya los trae; T-09 verifica que ninguna fila resoluble quede sin id y que ninguna fila por socio reciba ids de puesto |
| R-03 Catálogo caído | Medio | Ninguna tarea añade llamadas remotas al leer ni al listar; T-09 detiene catálogo y comprueba que `GET /api/cuentas` sigue devolviendo los ids desde el snapshot |
| R-09 Migraciones inconsistentes | Medio | `V5` es un archivo nuevo, consecutivo y no se edita después; T-02 verifica `success = 1` en `flyway_schema_history` y CA-09 exige aplicarla desde cero |
| R-10 Seeds y datos incoherentes | Medio | El relleno es `INNER JOIN` por igualdad exacta contra columnas únicas (`giros.nombre`, `socios.codigo`); las filas no resolubles quedan `NULL` y se cuentan, nunca se inventan valores. CA-06 comprueba que ningún id rellenado contradice al catálogo |
| R-12 Contratos divergentes | Medio | T-05 retira el javadoc que declaraba la desviación (dejaría de ser cierta) y CA-12 comprueba que `docs/openapi/cash.json` no cambia |
| Propio-01 (no catalogado en ESP-12) — la `V5` lee `gestion_catalog` | Medio | Lectura **de una sola vez, de solo lectura y dentro de una migración de datos históricos**, nunca en el camino de ejecución: ninguna operación de caja consulta catálogo al leer o listar, y CA-03 lo prueba con el servicio detenido. La migración no escribe en `gestion_catalog` ni crea claves foráneas, y CA-11 verifica que el esquema de caja sigue sin referencias externas. Si el orquestador considera que ESP-03 §1 también alcanza a las migraciones, la alternativa es ejecutar el mismo relleno como sentencia manual documentada en el entregable de T-02, sin cambiar el resto del plan |

## 6. Tareas

### T-01 — Reproducir el defecto y fijar la línea base de datos

- **Objetivo:** dejar constancia literal de los `null` y del volumen real de filas a rellenar antes de tocar el esquema.
- **Especificación:** ESP-04 §5.4; `docs/contract/baseline-contrato.md` §1.4; PLAN-04 §8 CA-02.
- **Archivos:**
  - `docs/evidencias/06-cuentas-ids-anidados-antes.txt` (nuevo)
- **Depende de:** ninguna.
- **Pasos:**
  1. Arrancar `identity-service`, `catalog-service`, `cash-service` y el gateway desde sus carpetas, con el `JWT_SECRET` de la shell.
  2. Obtener un token de `admin` por `POST /api/auth/login` en el gateway.
  3. Ejecutar `GET /api/cuentas` y localizar la cuenta con `puesto.numero = "P-101"`, `periodo = "2026-10"` y `servicio.id = 1`; guardar el objeto completo y señalar `puesto.socio.id` y `puesto.giro.id` en `null`.
  4. Ejecutar y guardar los tres conteos: total de `cuentas_cobrar`; cuentas con `puesto_id` no nulo; cuentas cuya fila de `gestion_catalog.puestos` existe para ese `puesto_id` (las demás son puestos ya eliminados del catálogo).
  5. Guardar además, en el mismo archivo, el conteo de filas con `puesto_socio_codigo`, el de filas con `puesto_giro_nombre` y el de filas de esas que hoy ya son resolubles contra `gestion_catalog.giros` y `gestion_catalog.socios`.
- **Verificación:** el archivo contiene el objeto de la cuenta del baseline con `"id":null` en los dos anidados y los cinco conteos, encabezados por el hash del commit sobre el que se ejecutaron.
- **Terminado cuando:** el defecto está reproducido en un archivo versionado y los cinco conteos existen con números concretos y su commit de referencia.

### T-02 — Crear la migración `V5` con las dos columnas y el relleno

- **Objetivo:** añadir `puesto_socio_id` y `puesto_giro_id` a `cuentas_cobrar` y reconstruir sus valores en las filas ya emitidas.
- **Especificación:** ESP-06 §1, §2, §4, §9; ESP-03 §4.3, §9, §10.
- **Archivos:**
  - `database/flyway/cash/V5__snapshot_ids_puesto.sql` (nuevo)
- **Depende de:** T-01.
- **Pasos:**
  1. Crear el archivo con cabecera de servicio, esquema y propósito, según ESP-06 §4, y con una nota que explique por qué el relleno lee `gestion_catalog` una sola vez: reconstruye el snapshot de documentos ya emitidos y nunca se repite en lectura.
  2. `ALTER TABLE cuentas_cobrar ADD COLUMN puesto_socio_id BIGINT NULL` y `ADD COLUMN puesto_giro_id BIGINT NULL`, ambas `NULL` porque una cuenta por socio no tiene puesto. Situarlas junto al bloque de snapshot del puesto.
  3. Rellenar el giro con un `UPDATE cuentas_cobrar c INNER JOIN gestion_catalog.giros g ON g.nombre = c.puesto_giro_nombre SET c.puesto_giro_id = g.id WHERE c.puesto_id IS NOT NULL`.
  4. Rellenar el socio del puesto con un `UPDATE cuentas_cobrar c INNER JOIN gestion_catalog.socios s ON s.codigo = c.puesto_socio_codigo SET c.puesto_socio_id = s.id WHERE c.puesto_id IS NOT NULL`.
  5. Usar `INNER JOIN` e igualdad exacta, nunca `LEFT JOIN`, `COALESCE`, `LIKE` ni valores por defecto: las filas cuyo giro o socio ya no exista en el catálogo quedan en `NULL` y siguen mostrando su snapshot de texto.
  6. No añadir clave foránea hacia `gestion_catalog` (ESP-03 §3) ni índices: las columnas no se usan en `WHERE`.
  7. No tocar `V1` a `V4` ni ningún archivo de otro servicio.
- **Verificación:** arrancar `cash-service`; `SELECT version, description, success FROM flyway_schema_history WHERE version = '5'` devuelve `success = 1`; `SHOW COLUMNS FROM cuentas_cobrar LIKE 'puesto%\_id'` lista las dos columnas; Hibernate valida sin error con `ddl-auto=validate`.
- **Terminado cuando:** la `V5` está aplicada con `success = 1`, las dos columnas existen y el servicio arranca con `validate`.

### T-03 — Mapear las dos columnas en la entidad

- **Objetivo:** que JPA conozca `puesto_socio_id` y `puesto_giro_id` sin relaciones hacia catálogo.
- **Especificación:** ESP-03 §3; ESP-06 §1 (`validate`); PLAN-04 T-01.
- **Archivos:**
  - `microservices/cash-service/src/main/java/com/cibertec/gestion/cash/entities/CuentaCobrarEntity.java`
- **Depende de:** T-02.
- **Pasos:**
  1. Añadir `@Column(name = "puesto_giro_id") private Long puestoGiroId;` dentro del bloque «snapshot del puesto», inmediatamente después de `puestoGiroNombre`.
  2. Añadir `@Column(name = "puesto_socio_id") private Long puestoSocioId;` a continuación, antes de `puestoSocioCodigo`.
  3. Declarar ambas como `Long` sin `nullable = false`: son nulas en las cuentas por socio y en los puestos cuyo catálogo ya no existe.
  4. No añadir `@ManyToOne`, `@JoinColumn` ni ninguna relación hacia catálogo.
  5. No modificar las restricciones únicas ni los demás campos.
- **Verificación:** `mvn -q -DskipTests package` compila y, con `ddl-auto=validate` y `gestion_cash` migrado, el servicio arranca sin error de esquema.
- **Terminado cuando:** la entidad declara las dos columnas y `validate` no reporta diferencias.

### T-04 — Escribir los ids en el alta de la cuenta

- **Objetivo:** que toda cuenta creada a partir de esta versión nazca con los ids del puesto persistidos.
- **Especificación:** ESP-03 §4.3, §6; ESP-04 §5.4.
- **Archivos:**
  - `microservices/cash-service/src/main/java/com/cibertec/gestion/cash/services/CuentaCobrarService.java` (método `nuevaCuenta`)
- **Depende de:** T-03.
- **Pasos:**
  1. Dentro del bloque `if (puesto != null)`, añadir `.puestoGiroId(puesto.giro() == null ? null : puesto.giro().id())`.
  2. En el mismo bloque, añadir `.puestoSocioId(socioPuesto == null ? null : socioPuesto.id())`, usando el `SocioAnidado` que ya se extrae del `PuestoRemoto`.
  3. No añadir ninguna llamada nueva a `CatalogoClient`: los dos ids llegan en la respuesta de `GET /api/puestos/{id}` que el alta ya hace, y las altas masivas por puesto también.
  4. No alterar las validaciones, los montos, los estados ni los snapshots existentes.
- **Verificación:** crear una cuenta por puesto devuelve 201 y, al releerla con `GET /api/cuentas/puesto/{id}`, los dos ids vienen informados; una cuenta por socio sigue devolviendo `puesto` en `null`.
- **Terminado cuando:** un alta nueva persiste los dos ids y ninguna otra regla del método cambió.

### T-05 — Proyectar los ids en el mapper y retirar la desviación documentada

- **Objetivo:** que la respuesta use los ids persistidos y que el código deje de declarar una desviación que ya no existe.
- **Especificación:** ESP-04 §5.4, §7 (reglas 1 a 5); ESP-13 §3, §4.
- **Archivos:**
  - `microservices/cash-service/src/main/java/com/cibertec/gestion/cash/mappers/CuentaCobrarMapper.java`
  - `microservices/cash-service/src/main/java/com/cibertec/gestion/cash/controllers/CuentaCobrarController.java`
- **Depende de:** T-03.
- **Pasos:**
  1. En `toResponse`, sustituir el `null` del primer argumento de `SocioAnidado` del puesto por `entity.getPuestoSocioId()`.
  2. Sustituir el `null` del primer argumento de `GiroAnidado` por `entity.getPuestoGiroId()`.
  3. Conservar intactas las condiciones que deciden cuándo `puesto` o `puesto.socio` son `null`, y conservar los nombres de campo de los `record` (`PuestoAnidado`, `SocioAnidado`, `GiroAnidado`).
  4. En `CuentaCobrarMapper`, sustituir el javadoc «Desviación conocida (CA-02 de PLAN-04)» por un texto que describa el estado final: los ids del socio y del giro del puesto forman parte del snapshot y se leen de `puesto_socio_id` y `puesto_giro_id`.
  5. En `CuentaCobrarController`, sustituir el párrafo «Desviación documentada de ESP-04 §5.4» por la referencia a la migración `V5`, sin repetir la afirmación de que los ids viajan en `null`.
  6. No modificar firmas, rutas, códigos de estado ni la forma de ningún `record`.
- **Verificación:** `git diff` sobre ambos archivos no muestra ningún cambio en nombres de campo ni en anotaciones de ruta; `GET /api/cuentas` devuelve `puesto.socio.id` y `puesto.giro.id` informados para la cuenta del baseline.
- **Terminado cuando:** el mapper lee los dos ids de la entidad y no queda en el código ninguna mención a la desviación de CA-02.

### T-06 — Cubrir los ids anidados con pruebas

- **Objetivo:** que una regresión de los dos ids se detecte en `mvn test`.
- **Especificación:** ESP-13 §5, §5.1; ESP-12 R-01, R-02.
- **Archivos:**
  - `microservices/cash-service/src/test/java/com/cibertec/gestion/cash/services/CuentaCobrarServiceTest.java` (solo se añaden pruebas y sus utilidades)
- **Depende de:** T-04, T-05.
- **Pasos:**
  1. Añadir `debePersistirLosIdsAnidadosDelPuesto`: crear una cuenta por puesto, releerla desde `CuentaCobrarRepository` y afirmar que `puestoGiroId` y `puestoSocioId` valen los ids que devuelve el `PuestoRemoto` mockeado.
  2. Añadir en la misma prueba que la respuesta de `create` trae `puesto().socio().id()` y `puesto().giro().id()` distintos de `null`.
  3. Añadir `cuentaPorSocioNoDebeTraerIdsDePuesto`: crear una cuenta por socio y afirmar que `puesto()` es `null` y que las dos columnas nuevas quedan `null` en la entidad.
  4. Reutilizar el mock de catálogo existente (`puesto()` ya devuelve `GiroAnidado(1L, "Abarrotes y víveres")` y `SocioAnidado(1L, ...)`); no añadir dependencias al POM.
  5. No modificar las siete pruebas existentes ni los dos `@BeforeEach`.
- **Verificación:** desde `microservices/cash-service`, `mvn test` devuelve `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.
- **Terminado cuando:** las nueve pruebas pasan y las siete anteriores no cambiaron.

### T-07 — Añadir los asserts de los ids en Postman

- **Objetivo:** que el contrato ejecutable detecte los ids en `null` en la próxima corrida.
- **Especificación:** ESP-09 §4.3, §4.8, §8; ESP-04 §5.4.
- **Archivos:**
  - `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-05.
- **Pasos:**
  1. En `03 · Caja → Crear cuenta por puesto`, añadir al `test` existente dos asserts: `creada.puesto.giro.id` igual a `{{giroId}}` y `creada.puesto.socio.id` igual a `{{socioId}}`, ambos comparados como número para no depender del tipo con que Postman guarde la variable.
  2. En `03 · Caja → Listar cuentas (contrato ESP-09 4.8)`, añadir sobre la cuenta encontrada: `puesto.giro.id` numérico y mayor que 0, y `puesto.socio.id` numérico y mayor que 0.
  3. En el mismo ítem, añadir un assert de que `puesto.socio.codigo` y `puesto.giro.nombre` siguen presentes, para probar que el arreglo no sustituyó el snapshot de texto por el id.
  4. No tocar las carpetas `00`, `01`, `02` ni `04`, ni las variables de colección o de entorno.
  5. No añadir peticiones nuevas.
- **Verificación:** `npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json -e docs/postman/Local_Microservicios.postman_environment.json` termina con 0 fallos y los cuatro asserts nuevos aparecen en el informe.
- **Terminado cuando:** la colección pasa completa con los asserts de ids y sin peticiones añadidas.

### T-08 — Alinear la precondición de migraciones de PLAN-05

- **Objetivo:** que PLAN-05 no exija un estado de `gestion_cash` que la `V5` ya supera.
- **Especificación:** ESP-06 §4; `docs/plans/README.md` §8.1.
- **Archivos:**
  - `docs/plans/05-cash-service-tesoreria-reportes.md` (§4, una fila)
- **Depende de:** T-02.
- **Pasos:**
  1. En la tabla de §4, cambiar la fila «`gestion_cash` con `V1` a `V4` aplicadas» por «`gestion_cash` con `V1` a `V4` aplicadas, más `V5` si PLAN-09 ya se ejecutó».
  2. En la misma sección, añadir debajo de la tabla una línea que diga que PLAN-05 no crea migraciones en `cash` y que, si alguna vez necesita una, tomará el número siguiente al máximo aplicado (`V6` si `V5` existe).
  3. No cambiar el estado de PLAN-05, ni sus tareas, ni sus criterios, ni el número de migración `V4` que ya menciona.
- **Verificación:** `Select-String -Path docs/plans/05-cash-service-tesoreria-reportes.md -Pattern 'V5','V6'` devuelve al menos dos coincidencias y `git diff` de ese archivo muestra solo esas líneas.
- **Terminado cuando:** PLAN-05 admite `V5` como estado válido y no se modificó nada más del plan.

### T-09 — Re-verificar el CA-02 de PLAN-04 y la degradación por catálogo caído

- **Objetivo:** demostrar que el criterio roto se cumple y que se cumple por snapshot, no por llamada a catálogo.
- **Especificación:** ESP-04 §5.4; ESP-03 §5, §8; PLAN-04 §8 CA-02 y CA-08.
- **Archivos:**
  - `docs/evidencias/07-cuentas-ids-anidados-despues.txt` (nuevo)
- **Depende de:** T-02, T-04, T-05, T-06, T-07.
- **Pasos:**
  1. Comparar la cuenta del baseline (`puesto.numero = "P-101"`, `periodo = "2026-10"`, `servicio.id = 1`) con `docs/contract/baseline-contrato.md` §1.4 campo por campo: los once campos de primer nivel, los cinco de `servicio`, los cuatro de `puesto.socio` y los dos de `puesto.giro`, con `puesto.socio.id = 1` y `puesto.giro.id = 1`.
  2. Ejecutar las dos consultas de «no queda ninguna fila resoluble sin id» y la de «ningún id contradice al catálogo» de §9, y guardar los resultados.
  3. Contar y guardar las filas no resolubles (puesto que ya no existe en `gestion_catalog`): deben ser exactamente las mismas de T-01 y conservar `puesto_socio_id` y `puesto_giro_id` en `null`.
  4. Detener `catalog-service` y comprobar que `GET /api/cuentas` y `GET /api/cuentas/puesto/1` responden 200 con los ids informados.
  5. Con catálogo caído, comprobar que `POST /api/cuentas` sigue respondiendo 503 sin insertar filas (CA-07 de PLAN-04 no se rompió).
  6. Reiniciar `catalog-service`.
  7. Escribir en el archivo de evidencia el resultado de los seis pasos y la referencia explícita a «CA-02 de PLAN-04: cumple».
- **Verificación:** la comparación no muestra diferencias, las tres consultas devuelven `0`, el conteo de no resolubles coincide con T-01 y las tres respuestas con catálogo caído son las esperadas.
- **Terminado cuando:** el CA-02 de PLAN-04 se declara cumplido con evidencia literal y la degradación no cambió.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Sin reproducción no hay forma de comprobar el arreglo |
| 2 | T-02 | La migración y el relleno van antes que cualquier clase Java que los use |
| 3 | T-03 | La entidad necesita las columnas ya creadas por `V5` |
| 4 | T-04, T-05 | Escritura y lectura de los ids; ambos dependen de la entidad |
| 5 | T-06, T-08 | Pruebas del camino completo y alineación textual de PLAN-05, en paralelo |
| 6 | T-07 | Los asserts de Postman se escriben sobre el JSON ya corregido |
| 7 | T-09 | Re-verificación final contra el baseline y con catálogo caído |

T-02 debe aplicarse antes de arrancar `cash-service` con la entidad de T-03: si el
orden se invierte, `ddl-auto=validate` falla porque las columnas todavía no
existen en `gestion_cash`.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | La cuenta del baseline devuelve `puesto.socio.id = 1` y `puesto.giro.id = 1` | Comparación campo por campo de `GET /api/cuentas` contra `baseline-contrato.md` §1.4: los once campos de primer nivel, los cinco de `servicio`, los cuatro de `puesto.socio` y los dos de `puesto.giro`, sin diferencias | ESP-04 §5.4 |
| CA-02 | La forma del JSON no cambió: mismos once campos de primer nivel, mismos anidados y mismos `null` explícitos | La comparación de CA-01 no reporta campos añadidos, renombrados ni omitidos | ESP-04 §7 |
| CA-03 | Listar y consultar por puesto funcionan con catálogo caído, con los ids informados | `GET /api/cuentas` y `GET /api/cuentas/puesto/1` responden 200 con `catalog-service` detenido | ESP-03 §5, §8 |
| CA-04 | Con catálogo caído el alta sigue devolviendo 503 sin insertar | Conteo de `cuentas_cobrar` antes y después del alta fallida | ESP-03 §8 |
| CA-05 | Ninguna fila resoluble quedó sin id tras la migración | Las dos consultas de «no queda fila resoluble sin id» de §9 devuelven `0` | ESP-04 §5.4; ESP-03 §4.3 |
| CA-06 | Ningún id rellenado contradice al catálogo | La consulta de coherencia de §9 devuelve `0` | ESP-03 §9, §10 |
| CA-07 | Las filas no resolubles quedan cuantificadas y conservan su snapshot de texto sin id inventado | La consulta de «no resolubles» de §9 devuelve un conteo que coincide con el registrado en T-01, y `puesto_numero`, `puesto_socio_codigo` y `puesto_giro_nombre` siguen no nulos en esas filas | ESP-03 §10; ESP-12 R-10 |
| CA-08 | Una cuenta nueva nace con los dos ids | La respuesta 201 de `POST /api/cuentas` trae ambos ids y la relectura los conserva | ESP-03 §4.3, §6 |
| CA-09 | `V5` aplica desde cero y `success = 1` | `gestion_cash` recreada: `flyway_schema_history` con 1 a 5 y todas en `success = 1` | ESP-06 §4 |
| CA-10 | El esquema real coincide con la entidad | `cash-service` arranca con `ddl-auto=validate` | ESP-06 §1 |
| CA-11 | No hay claves foráneas nuevas hacia otros esquemas | `information_schema.referential_constraints` de `gestion_cash` solo contiene `fk_recibos_cuenta` | ESP-03 §3 |
| CA-12 | El contrato exportado no cambió | `git diff --exit-code docs/openapi/cash.json` sin cambios, y `GiroAnidado.id` y `SocioAnidado.id` siguen `int64` | ESP-08 §8 |
| CA-13 | Las pruebas de caja pasan | `mvn test` con `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` | ESP-13 §5.1 |
| CA-14 | La colección pasa con los asserts de ids | Newman de la colección completa con 0 fallos | ESP-09 §4.8 |
| CA-15 | La autorización no cambió | `GET /api/cuentas` sin token responde 401 | ESP-05 §5 |
| CA-16 | PLAN-05 admite el nuevo estado de migraciones | `select-string` sobre su §4 devuelve las menciones a `V5` y `V6` | ESP-06 §4 |

## 9. Verificación

```powershell
# JWT_SECRET se toma del entorno local; no se escribe en el plan (contrato de planes, sección 4.2)
if (-not $env:JWT_SECRET) { throw 'Defina JWT_SECRET antes de arrancar los servicios' }
$token = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token

# CA-01 y CA-02 — comparación con baseline-contrato.md §1.4
curl.exe -s http://localhost:9101/api/cuentas -H "Authorization: Bearer $token" |
  ConvertFrom-Json |
  Where-Object { $_.puesto.numero -eq 'P-101' -and $_.periodo -eq '2026-10' -and $_.servicio.id -eq 1 } |
  Select-Object -First 1 |
  ConvertTo-Json -Depth 6
# Esperado: puesto.socio.id = 1, puesto.socio.codigo = "SOC-001", puesto.giro.id = 1,
# puesto.giro.nombre = "Abarrotes y víveres", socio = null, lecturaFinal = null.

# CA-03 y CA-04 — con catalog-service detenido
curl.exe -i -s http://localhost:9101/api/cuentas -H "Authorization: Bearer $token" | Select-Object -First 1
curl.exe -i -s http://localhost:9101/api/cuentas/puesto/1 -H "Authorization: Bearer $token" | Select-Object -First 1
mysql -u gestion_user -p gestion_cash -e "SELECT COUNT(*) FROM cuentas_cobrar;"
curl.exe -i -s -X POST http://localhost:9101/api/cuentas -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{\"periodo\":\"2026-11\",\"puestoId\":1,\"servicioId\":1,\"monto\":80}' | Select-Object -First 1
mysql -u gestion_user -p gestion_cash -e "SELECT COUNT(*) FROM cuentas_cobrar;"

# CA-05 — ninguna fila resoluble sin id
mysql -u gestion_user -p gestion_cash -e "
SELECT COUNT(*) AS giro_pendiente
FROM cuentas_cobrar c
WHERE c.puesto_id IS NOT NULL
  AND c.puesto_giro_nombre IS NOT NULL
  AND c.puesto_giro_id IS NULL
  AND EXISTS (SELECT 1 FROM gestion_catalog.giros g WHERE g.nombre = c.puesto_giro_nombre);

SELECT COUNT(*) AS socio_pendiente
FROM cuentas_cobrar c
WHERE c.puesto_id IS NOT NULL
  AND c.puesto_socio_codigo IS NOT NULL
  AND c.puesto_socio_id IS NULL
  AND EXISTS (SELECT 1 FROM gestion_catalog.socios s WHERE s.codigo = c.puesto_socio_codigo);
"
# Esperado: 0 y 0.

# CA-06 — ningún id contradice al catálogo
mysql -u gestion_user -p gestion_cash -e "
SELECT COUNT(*) AS giro_incoherente
FROM cuentas_cobrar c
LEFT JOIN gestion_catalog.giros g ON g.id = c.puesto_giro_id
WHERE c.puesto_giro_id IS NOT NULL AND (g.id IS NULL OR g.nombre <> c.puesto_giro_nombre);

SELECT COUNT(*) AS socio_incoherente
FROM cuentas_cobrar c
LEFT JOIN gestion_catalog.socios s ON s.id = c.puesto_socio_id
WHERE c.puesto_socio_id IS NOT NULL AND (s.id IS NULL OR s.codigo <> c.puesto_socio_codigo);
"
# Esperado: 0 y 0.

# CA-07 — filas no resolubles y cuentas por socio
mysql -u gestion_user -p gestion_cash -e "
SELECT COUNT(*) AS huerfanas
FROM cuentas_cobrar c
LEFT JOIN gestion_catalog.puestos p ON p.id = c.puesto_id
WHERE c.puesto_id IS NOT NULL AND p.id IS NULL;

SELECT COUNT(*) AS no_resoluble_sin_snapshot
FROM cuentas_cobrar c
LEFT JOIN gestion_catalog.puestos p ON p.id = c.puesto_id
WHERE (c.puesto_id IS NOT NULL AND p.id IS NULL)
  AND (c.puesto_numero IS NULL OR c.puesto_giro_nombre IS NULL);

SELECT COUNT(*) AS socio_con_ids_de_puesto
FROM cuentas_cobrar
WHERE puesto_id IS NULL AND (puesto_socio_id IS NOT NULL OR puesto_giro_id IS NOT NULL);
"
# Esperado: no_resoluble_sin_snapshot = 0 (el texto histórico se conserva) y
# socio_con_ids_de_puesto = 0. El conteo de no resolubles se compara con T-01.

# CA-08 — una cuenta nueva nace con los dos ids (catálogo arriba, periodo nuevo)
curl.exe -s -X POST http://localhost:9101/api/cuentas -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{\"periodo\":\"2026-11\",\"puestoId\":1,\"servicioId\":1,\"monto\":80}' |
  ConvertFrom-Json | Select-Object id, @{n='socioId';e={$_.puesto.socio.id}}, @{n='giroId';e={$_.puesto.giro.id}}
# Esperado: socioId y giroId numéricos, no nulos.
mysql -u gestion_user -p gestion_cash -e "
SELECT id, puesto_id, puesto_socio_id, puesto_giro_id FROM cuentas_cobrar WHERE periodo = '2026-11';"
# Esperado: mismas dos columnas informadas en la fila recién creada.

# CA-10 — el esquema real coincide con la entidad
# Arrancar cash-service desde su carpeta con ddl-auto=validate y detenerlo después.
cd microservices/cash-service ; mvn spring-boot:run
# Esperado: el contexto arranca sin «Schema-validation: missing column» y sin
# «Schema-validation: wrong column type».

# CA-09 — desde cero
# mysql -u root -p -e "DROP DATABASE gestion_cash;"
# mysql -u root -p < database/crear_base_microservicios.sql
# cd microservices/cash-service ; mvn spring-boot:run
mysql -u gestion_user -p gestion_cash -e "
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
# Esperado: 1..5 con success = 1.

# CA-11 — sin claves foráneas nuevas
mysql -u gestion_user -p -e "
SELECT constraint_schema, table_name, constraint_name
FROM information_schema.referential_constraints
WHERE constraint_schema = 'gestion_cash';"
# Esperado: solo fk_recibos_cuenta.

# CA-12 — el contrato exportado no cambió
git diff --exit-code docs/openapi/cash.json

# CA-13
cd microservices/cash-service ; mvn test

# CA-14
npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json `
  -e docs/postman/Local_Microservicios.postman_environment.json

# CA-15
curl.exe -i -s http://localhost:9101/api/cuentas | Select-Object -First 1

# CA-16
Select-String -Path docs/plans/05-cash-service-tesoreria-reportes.md -Pattern 'V5','V6'
```

## 10. Reversa

| Elemento | Cómo se revierte |
|---|---|
| `V5__snapshot_ids_puesto.sql` ya aplicada | En desarrollo, `DROP DATABASE gestion_cash` y reaplicar `V1` a `V4`; **nunca** editar ni borrar la `V5` aplicada (ESP-06 §4, ESP-12 R-09) |
| Entidad, servicio y mapper | Revertir el commit del plan; el servicio vuelve a leer `null` y el JSON recupera la forma anterior sin cambios de esquema |
| Pruebas de T-06 | Eliminar las dos pruebas añadidas; las siete existentes no se tocaron |
| Asserts de T-07 | Revertir el commit de la colección; las peticiones no cambiaron, solo sus `test` |
| Fila de T-08 en PLAN-05 | Restaurar el texto anterior de la precondición |
| Todo el plan | El gateway puede volver a enrutar `cuentas` al monolito (ESP-11 §8); el monolito conserva `sistema_gestion` intacto |

La reversa es total porque las dos columnas son nuevas y solo las consume la
proyección de esta cuenta: al volver al estado sin `V5` —`DROP DATABASE` y
reaplicar `V1` a `V4`, como en el resto de planes— y revertir el commit de
código, el JSON recupera exactamente la forma documentada en la evidencia
`04-cash-service-cobranza-bloqueo.md` y ninguna fila de negocio se pierde, porque
`V1` a `V4` reconstruyen los mismos seeds. La `V5` aplicada no se edita ni se
borra nunca (ESP-06 §4).

## 11. Entregables

```text
database/flyway/cash/V5__snapshot_ids_puesto.sql          (nuevo, no se edita después)
microservices/cash-service/src/main/java/com/cibertec/gestion/cash/
├─ entities/CuentaCobrarEntity.java                       (puestoGiroId, puestoSocioId)
├─ mappers/CuentaCobrarMapper.java                        (ids desde la entidad; sin javadoc de desviación)
├─ services/CuentaCobrarService.java                      (nuevaCuenta escribe los ids)
└─ controllers/CuentaCobrarController.java                (javadoc alineado)
microservices/cash-service/src/test/java/com/cibertec/gestion/cash/services/CuentaCobrarServiceTest.java
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json   (asserts de ids)
docs/evidencias/06-cuentas-ids-anidados-antes.txt         (nuevo)
docs/evidencias/07-cuentas-ids-anidados-despues.txt       (nuevo)
docs/plans/05-cash-service-tesoreria-reportes.md          (§4, una fila y una nota)
docs/plans/README.md                                      (índice §9 y tabla §8.2)
```

Comportamientos observables al cerrar:

- `GET /api/cuentas` devuelve `puesto.socio.id` y `puesto.giro.id` numéricos en
  toda cuenta cuyo puesto y giro sigan existiendo en el catálogo.
- Los listados no hacen ninguna llamada a catálogo y funcionan con el servicio
  detenido.
- Toda cuenta nueva nace con los dos ids persistidos.

## 12. Fuera de alcance

- Cambiar `docs/contract/baseline-contrato.md`, ESP-04 §5.4 o cualquier nombre
  de campo del JSON.
- Resolver los dos ids al leer, incluso como caché o con una sola llamada
  agrupada: prohibido por ESP-03 §5 y §8.
- Inventar ids para las filas cuyo puesto o socio ya no existe en el catálogo:
  se quedan en `null` con su snapshot de texto, que es el estado histórico
  correcto (ESP-03 §10).
- Editar `V1` a `V4`, `database/crear_base.sql` o `backend/`.
- Crear claves foráneas hacia `gestion_catalog` o índices sobre las dos columnas.
- Añadir la fila correspondiente a `puesto_socio_id` y `puesto_giro_id` en
  ESP-03 §4.3: es una decisión humana pendiente (ver entrega del planificador);
  no bloquea la ejecución porque §4.3 enumera lo que caja guarda y no prohíbe
  columnas adicionales, mientras ESP-04 §5.4 exige esos dos valores.
- Añadir `V5__snapshot_ids_puesto.sql` al árbol de `cash` de ESP-06 §2: también
  es decisión humana; el plan solo deja constancia en el índice §9 de
  `docs/plans/README.md` y en la precondición de PLAN-05.
- Recalcular, migrar o limpiar los snapshots de texto de las cuentas antiguas.
- Depurar las 427 cuentas y 282 recibos acumulados por reejecuciones de Postman
  (nota de cierre de PLAN-06/07).
- Cualquier cambio en egresos, reportes, recibos, correlativos, seguridad,
  puertos, gateway o frontend.
- Hacer merge, `rebase`, `amend` o `force-push`: la rama y el PR #9 son del
  orquestador.
