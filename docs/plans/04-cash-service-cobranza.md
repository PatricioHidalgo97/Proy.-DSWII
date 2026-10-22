# PLAN-04 — cash-service: cuentas por cobrar, recibos y correlativos

## 1. Objetivo

Extraer la cobranza a un servicio independiente sobre `gestion_cash`: cuentas
por cobrar, recibos y correlativos, con snapshots de catálogo y del usuario del
JWT en lugar de claves foráneas remotas. Este plan cierra los 14 endpoints de
ESP-04 §4.3 y §4.4.

Es la primera mitad de caja. La segunda —egresos y reportes— es el PLAN-05, que
aprovecha la misma base, los mismos correlativos y el mismo cliente de catálogo.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-01 | §3.3, §6, §7 | Cohesión del dominio del dinero; caja como servicio mayor |
| ESP-03 | §1, §3, §4.1, §4.2, §4.3, §4.4, §5, §6, §7, §8, §10 | Snapshots, claves remotas, propagación de identidad, 503 |
| ESP-04 | §4.3, §4.4, §5.4, §5.5, §6, §7 | Contrato de cuentas y recibos |
| ESP-05 | §3, §4, §5, §9, §11 | `UsuarioPrincipal`, propagación del token, reglas |
| ESP-06 | §1, §2, §3, §5 | `gestion_cash`, Flyway `cash` V1–V3 |
| ESP-08 | §4, §5, §7.3, §8 | Swagger de cuentas y recibos |
| ESP-11 | §3 (F3), §4.3, §4.4, §7 (Fase 3), §8 | Qué se mueve y transformaciones |
| ESP-12 | R-01, R-02, R-03, R-04, R-05 | Riesgos críticos de este plan |
| ESP-13 | §1.2, §3, §5, §5.1 | Nomenclatura, estilo y pruebas |

## 3. Alcance

### Dentro

- Entidades `CuentaCobrarEntity`, `ReciboEntity`, `CorrelativoEntity` con columnas de snapshot.
- Repositorios con `findByIdForUpdate` y `findByTipoForUpdate`.
- `CorrelativoService` transaccional.
- `CatalogoClient` con timeout corto, propagación del token y traducción a 503.
- `ICuentaCobrarService` con sus once operaciones.
- `IReciboService` con sus tres operaciones.
- Cascarón de `cash-service` con seguridad, errores y springdoc.
- Migración `V1` y `V2` y `V3` aplicadas desde el servicio.
- Pruebas equivalentes a `CuentaCobrarServiceTest`.
- Carpetas `03 · Caja` y `04 · Recibos` de Postman.
- Retiro de los prefijos `cuentas` y `recibos` de la ruta `monolito`.

### Fuera

- Egresos, importación CSV y reportes (PLAN-05).
- `EgresoEntity`, `ReporteService` y el controlador de reportes.
- Sincronización de datos con el monolito.
- Reintentos automáticos de llamadas a catálogo.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-00, PLAN-01 y PLAN-03 cerrados | Índice de `docs/plans/README.md` |
| Catálogo responde los cinco recursos por el gateway | `GET /api/socios`, `/puestos`, `/servicios`, `/bancos` |
| `gestion_cash` con `V1`, `V2` y `V3` aplicadas | `flyway_schema_history` |
| 344 cuentas y 216 recibos sembrados | Conteos de PLAN-00 §9.1 |
| Correlativo `RECIBO = 216` | `SELECT ultimo_numero FROM correlativos WHERE tipo='RECIBO'` |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-09 compara `cuenta` y `recibo` con ESP-04 §5.4 y §5.5; T-11 lo verifica en Postman |
| R-02 Snapshots incompletos | Alto | T-03 hace `NOT NULL` lo que ESP-03 §4 exige y valida al insertar; T-11 verifica que ningún recibo tenga `usuario_nombres` vacío |
| R-03 Catálogo caído bloquea altas | Medio | T-04 traduce timeout a 503 sin insertar; T-12 prueba detener catálogo y confirma que listar y pagar siguen funcionando |
| R-04 Secretos JWT distintos | Alto | T-06 valida el mismo token que emitió identidad, sin CORS y con el mismo `JWT_SECRET` |
| R-05 Correlativos duplicados | Alto | T-02 conserva `findByTipoForUpdate`; T-10 agrega una prueba de dos pagos concurrentes |

## 6. Tareas

### T-01 — Crear las entidades con snapshots

- **Objetivo:** mapear `cuentas_cobrar`, `recibos` y `correlativos` sin claves foráneas remotas.
- **Especificación:** ESP-03 §3, §4; ESP-06 §1.
- **Archivos:**
  - `.../cash/entities/{CuentaCobrarEntity,ReciboEntity,CorrelativoEntity}.java`
  - `.../cash/enums/{EstadoCuenta,TipoRecibo}.java`
- **Depende de:** PLAN-00.
- **Pasos:**
  1. Partir de las entidades del monolito y **sustituir cada `@ManyToOne` a catálogo o a usuario por un `Long` más columnas de snapshot**, tal como indica ESP-11 §4.4.
  2. En `CuentaCobrarEntity`: `puestoId`, `socioId`, `servicioId` más las columnas de ESP-03 §4.2, §4.3 y §4.4.
  3. En `ReciboEntity`: `cuentaCobrar` como `@OneToOne` local; `bancoId` y las cuatro columnas de banco; `usuarioId` y las cuatro columnas de usuario.
  4. Marcar `NOT NULL` las columnas de servicio y de usuario; dejar nulas las de puesto y socio cuando no apliquen.
  5. Conservar `uk_cuentas_puesto`, `uk_cuentas_socio` y `uk_recibos_cuenta`.
- **Verificación:** `ddl-auto=validate` no falla contra `gestion_cash`.
- **Terminado cuando:** Hibernate valida las tres tablas sin ninguna relación remota.

### T-02 — Implementar correlativos

- **Objetivo:** numeración transaccional sin colisiones.
- **Especificación:** ESP-12 R-05; ESP-13 §3.
- **Archivos:**
  - `.../cash/repositories/CorrelativoRepository.java`
  - `.../cash/services/CorrelativoService.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar `CorrelativoRepository` con `findByTipoForUpdate`.
  2. Copiar `CorrelativoService.next` sin cambios: bloqueo pesimista, incremento, `saveAndFlush`.
  3. Conservar el formato `%s-%06d`.
  4. No leer el máximo de la tabla de recibos: el correlativo manda.
- **Verificación:** dos llamadas consecutivas devuelven `REC-000217` y `REC-000218`.
- **Terminado cuando:** la numeración continúa desde el seed sin saltos.

### T-03 — Implementar cuentas por cobrar

- **Objetivo:** las once operaciones de ESP-04 §4.3 con snapshots completos.
- **Especificación:** ESP-04 §4.3, §5.4; ESP-03 §4.2, §4.3, §4.4, §6.
- **Archivos:**
  - `.../cash/repositories/CuentaCobrarRepository.java`
  - `.../cash/services/{ICuentaCobrarService,CuentaCobrarService}.java`
  - `.../cash/controllers/CuentaCobrarController.java`
  - `.../cash/dto/{CuentaRequest,GenerarCuentasRequest,OperacionLoteRequest,CanjeRequest}.java`
- **Depende de:** T-01, T-02.
- **Pasos:**
  1. Copiar las reglas intactas: XOR puesto/socio, `cargoA` correcto, duplicado por período, cálculo por consumo, servicio inactivo.
  2. Reemplazar cada `puestoRepository`/`socioRepository`/`servicioRepository` por una llamada a `CatalogoClient`.
  3. Al insertar, escribir el snapshot completo desde la respuesta de catálogo.
  4. Conservar `findByIdForUpdate` en `getPendingAccount`.
  5. En `pay` y `payBatch`, construir el snapshot del usuario desde `UsuarioPrincipal`, no desde la base.
  6. Mantener los códigos 201 y 400 con los mensajes actuales.
- **Verificación:** crear una cuenta de puesto devuelve 201 con `puesto`, `socio` y `servicio` anidados y los cinco campos de snapshot persistidos.
- **Terminado cuando:** las once operaciones responden igual que el monolito.

### T-04 — Implementar `CatalogoClient`

- **Objetivo:** leer catálogo por HTTP con el token del usuario.
- **Especificación:** ESP-03 §6, §7, §8; ESP-01 §4.
- **Archivos:**
  - `.../cash/clients/CatalogoClient.java`
  - `.../cash/clients/dto/{PuestoRemoto,SocioRemoto,ServicioRemoto,BancoRemoto}.java`
- **Depende de:** T-03.
- **Pasos:**
  1. Usar `RestClient` o `WebClient` bloqueante contra `${app.clients.catalog.url}`.
  2. Reenviar el header `Authorization` del request actual; no usar credenciales técnicas.
  3. Implementar `getServicio`, `getPuesto`, `getSocio`, `getBanco`, `listPuestos` y `listSocios`.
  4. Timeout corto y configurable por propiedad.
  5. Traducir: 404 a `NotFoundException`, 401/403 al mismo código, timeout o conexión rechazada a 503 con mensaje de dependencia no disponible.
  6. No implementar reintentos.
- **Verificación:** con catálogo detenido, un alta devuelve 503 y no inserta ninguna fila.
- **Terminado cuando:** los tres escenarios de ESP-03 §8 se comportan como se describe.

### T-05 — Implementar recibos e ingresos externos

- **Objetivo:** las tres operaciones de ESP-04 §4.4.
- **Especificación:** ESP-04 §4.4, §5.5; ESP-03 §4.1, §4.5.
- **Archivos:**
  - `.../cash/repositories/ReciboRepository.java`
  - `.../cash/services/{IReciboService,ReciboService}.java`
  - `.../cash/controllers/ReciboController.java`
  - `.../cash/dto/IngresoExternoRequest.java`
- **Depende de:** T-02, T-03.
- **Pasos:**
  1. Copiar `getById` y `getByDate` con el filtro opcional por tipo.
  2. En `createExternalIncome`, tomar el usuario de `UsuarioPrincipal` y escribir su snapshot.
  3. En el canje, validar el banco contra catálogo y guardar su snapshot.
  4. Conservar `INGRESO`, `INGRESO_EXTERNO` y `BANCARIO`.
- **Verificación:** un canje devuelve 201 con `banco` no nulo y `usuario` con nombres y apellidos.
- **Terminado cuando:** las tres operaciones responden con la forma de ESP-04 §5.5.

### T-06 — Configurar seguridad, errores y dependencias

- **Objetivo:** que el servicio valide JWT y tenga el esqueleto completo.
- **Especificación:** ESP-05 §3, §4, §5, §9, §11.
- **Archivos:**
  - `.../cash/security/{JwtService,JwtAuthenticationFilter,WebSecurityConfig,UsuarioPrincipal}.java`
  - `.../cash/exceptions/{ApiExceptionHandler,BusinessException,ConflictException,NotFoundException,DependenciaNoDisponibleException}.java`
  - `microservices/cash-service/pom.xml`
  - `microservices/cash-service/src/main/resources/application.properties`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar el filtro JWT con `UsuarioPrincipal`.
  2. Reglas: `OPTIONS`, Swagger y `/actuator/health` públicos; `PUT /api/egresos/*/anular` ADMIN; resto autenticado.
  3. Sin CORS.
  4. Agregar el handler que traduce `DependenciaNoDisponibleException` a 503.
  5. Declarar `app.clients.catalog.url` desde `CATALOG_URL`.
  6. Agregar al POM `spring-boot-starter-webflux` solo si se usa `WebClient`; con `RestClient` no hace falta.
- **Verificación:** `GET /api/cuentas` sin token devuelve 401; con catálogo caído, un alta devuelve 503.
- **Terminado cuando:** los dos códigos se comportan como se describe.

### T-07 — Actualizar el gateway

- **Objetivo:** que `cuentas` y `recibos` dejen de ir al monolito.
- **Especificación:** ESP-07 §4; ESP-11 §2.4.
- **Archivos:** `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** T-03, T-05.
- **Pasos:**
  1. Confirmar que la ruta `cash` apunta a 9104.
  2. Excluir `/api/cuentas/**` y `/api/recibos/**` del `Path` de la ruta `monolito`.
  3. Mantener `egresos` y `reportes` en el monolito hasta PLAN-05.
- **Verificación:** las pantallas de cuentas y recibos cargan desde 9104 y las de egresos siguen desde 9090.
- **Terminado cuando:** cobranza ya no toca el monolito.

### T-08 — Agregar springdoc y exportar el contrato

- **Objetivo:** documentar cuentas y recibos.
- **Especificación:** ESP-08 §4, §5, §7.3, §8.
- **Archivos:**
  - `.../cash/config/OpenApiConfig.java`
  - `docs/openapi/cash.json`
- **Depende de:** T-03, T-05.
- **Pasos:**
  1. Agregar springdoc 2.8.14.
  2. Declarar `@OpenAPIDefinition` y `@SecurityScheme(bearerAuth)`.
  3. Tags `Cuentas` y `Recibos`; documentar los endpoints de lote y los ejemplos de cuenta por puesto y por socio.
  4. Exportar por ahora solo los paths implementados; el archivo se completa en PLAN-05.
- **Verificación:** Swagger UI abre en 9104 y el JSON contiene los 13 paths y las 14 operaciones de este plan.
- **Terminado cuando:** el contrato parcial está exportado y es válido.

### T-09 — Alinear el contrato con la línea base

- **Objetivo:** verificar cuentas y recibos contra la línea base.
- **Especificación:** ESP-04 §5.4, §5.5, §7, §8.
- **Archivos:** ninguno.
- **Depende de:** T-03, T-05.
- **Pasos:**
  1. Comparar una cuenta por puesto con ESP-04 §5.4: `puesto` con `socio` y `giro`, `socio` nulo, `servicio` con sus cuatro campos, y `lecturaInicial`, `lecturaFinal` y `costoUnitario` presentes.
  2. Comparar un recibo con ESP-04 §5.5: `cuentaCobrar` con `servicio`, `socio` y `puesto`; `banco`; `usuario`.
  3. Verificar que los `null` se serializan y no se omiten.
- **Verificación:** las tres comprobaciones coinciden.
- **Terminado cuando:** no hay diferencias con la línea base.

### T-10 — Portar las pruebas de cobranza

- **Objetivo:** conservar las reglas de negocio con pruebas.
- **Especificación:** ESP-13 §5, §5.1; ESP-12 R-02, R-05.
- **Archivos:** `microservices/cash-service/src/test/java/.../cash/services/CuentaCobrarServiceTest.java`
- **Depende de:** T-03.
- **Pasos:**
  1. Copiar los cuatro casos de `backend/src/test/java/com/cibertec/gestion/services/CuentaCobrarServiceTest.java`.
  2. Mockear `CatalogoClient` en lugar de los repositorios de catálogo.
  3. Agregar un caso de snapshot incompleto que debe fallar al insertar.
  4. Agregar un caso de dos pagos concurrentes que produce correlativos distintos.
  5. Agregar un caso de catálogo caído que produce 503 y no inserta.
- **Verificación:** las siete pruebas pasan.
- **Terminado cuando:** cobranza tiene al menos una prueba por regla migrada.

### T-11 — Completar las carpetas de caja y recibos en Postman

- **Objetivo:** cubrir ESP-09 §4.3 y §4.4.
- **Especificación:** ESP-09 §3, §4.3, §4.4, §7.1.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-07.
- **Pasos:**
  1. Agregar cuenta individual por puesto y por socio, guardando ambos ids.
  2. Agregar generación masiva por puestos y por socios.
  3. Agregar consulta por puesto y por socio.
  4. Agregar exoneración individual y en lote.
  5. Agregar pago individual, pago en lote y canje.
  6. Agregar consulta de recibos por fecha, por id e ingreso externo.
  7. Incluir los cinco asserts de contrato de ESP-09 §4.8 y el caso de pago duplicado que devuelve 400.
- **Verificación:** ambas carpetas pasan completas en Newman.
- **Terminado cuando:** los 14 endpoints tienen al menos una petición con assert.

### T-12 — Verificar la degradación por catálogo caído

- **Objetivo:** demostrar el comportamiento de ESP-03 §8.
- **Especificación:** ESP-03 §8; ESP-12 R-03.
- **Archivos:** ninguno.
- **Depende de:** T-04, T-07.
- **Pasos:**
  1. Detener catálogo.
  2. Verificar que `GET /api/cuentas` y `POST /api/cuentas/{id}/pagar` siguen funcionando.
  3. Verificar que `POST /api/cuentas` devuelve 503 y no deja filas nuevas.
  4. Reiniciar catálogo.
- **Verificación:** los tres comportamientos se cumplen y el conteo de cuentas no cambió tras el 503.
- **Terminado cuando:** la degradación parcial está demostrada.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01, T-06 | Persistencia y esqueleto de seguridad |
| 2 | T-02, T-04 | Correlativos y cliente de catálogo van antes que cualquier servicio de negocio |
| 3 | T-03 | Cuentas, requiere T-02 y T-04 |
| 4 | T-05 | Recibos, requiere T-02 y T-03 |
| 5 | T-07, T-08, T-09, T-10 | Gateway, Swagger, contrato y pruebas |
| 6 | T-11, T-12 | Postman y degradación, sobre el sistema ya enrutado |

T-04 debe estar compilando antes de T-03: sin `CatalogoClient` no hay forma de
construir los snapshots.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | Los 14 endpoints responden por el gateway | Newman de `03 · Caja` y `04 · Recibos` en verde | ESP-04 §4.3, §4.4 |
| CA-02 | Una cuenta conserva sus tres anidamientos | Comparación con ESP-04 §5.4 | ESP-04 §5.4 |
| CA-03 | Un recibo conserva `cuentaCobrar`, `banco` y `usuario` | Comparación con ESP-04 §5.5 | ESP-04 §5.5 |
| CA-04 | Los snapshots son completos | `SELECT COUNT(*) FROM recibos WHERE usuario_nombres=''` = 0 | ESP-03 §4.1 |
| CA-05 | El correlativo continúa desde 216 | El primer pago devuelve `REC-000217` | ESP-06 §5.7 |
| CA-06 | No hay claves foráneas hacia otros esquemas | `information_schema` sin FK a `gestion_catalog` ni a `gestion_auth` | ESP-03 §3 |
| CA-07 | Catálogo caído devuelve 503 sin insertar | Conteo antes y después del alta fallida | ESP-03 §8 |
| CA-08 | Listar y pagar funcionan sin catálogo | Las dos llamadas responden 200 | ESP-03 §8 |
| CA-09 | El token se propaga con el usuario | Log de catálogo muestra la petición de caja con el JWT del usuario | ESP-03 §7 |
| CA-10 | Las reglas de autorización se cumplen | Operador no puede anular; nadie opera sin token | ESP-05 §5 |
| CA-11 | El contrato parcial está exportado | `docs/openapi/cash.json` con 13 paths y 14 operaciones | ESP-08 §8 |
| CA-12 | Las pruebas de cobranza pasan | `mvn test` en verde | ESP-13 §5.1 |
| CA-13 | Egresos siguen en el monolito | La pantalla de egresos carga | ESP-11 §7 Fase 3 |
| CA-14 | El health de 9104 responde `UP` sin token | `curl.exe http://localhost:9104/actuator/health` | ESP-13 §6 |

## 9. Verificación

```powershell
# JWT_SECRET se toma del entorno local; no se escribe en el plan (contrato de planes, seccion 4.2)
if (-not $env:JWT_SECRET) { throw 'Defina JWT_SECRET antes de arrancar los servicios' }
$token = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token

# CA-02
curl.exe -s http://localhost:9101/api/cuentas -H "Authorization: Bearer $token"
curl.exe -s http://localhost:9101/api/cuentas/puesto/1 -H "Authorization: Bearer $token"

# CA-04
mysql -u gestion_user -p gestion_cash -e "SELECT COUNT(*) FROM recibos WHERE usuario_nombres='' OR usuario_apellidos='';"

# CA-05
curl.exe -s -X POST http://localhost:9101/api/cuentas/1/pagar `
  -H "Authorization: Bearer $token" | ConvertFrom-Json | Select-Object correlativo

# CA-06
mysql -u gestion_user -p -e "
SELECT table_schema, table_name, constraint_name FROM information_schema.referential_constraints
WHERE constraint_schema = 'gestion_cash';"

# CA-07 y CA-08 (con catálogo detenido)
mysql -u gestion_user -p gestion_cash -e "SELECT COUNT(*) FROM cuentas_cobrar;"
curl.exe -i -X POST http://localhost:9101/api/cuentas `
  -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
  -d '{\"periodo\":\"2026-10\",\"puestoId\":1,\"servicioId\":1,\"monto\":80}'
mysql -u gestion_user -p gestion_cash -e "SELECT COUNT(*) FROM cuentas_cobrar;"

# CA-12
cd microservices/cash-service ; mvn test

# CA-14
curl.exe http://localhost:9104/actuator/health
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 a T-06 | Detener caja; el gateway vuelve a enrutar `cuentas` y `recibos` al monolito |
| T-07 | Restaurar `/api/**` en la ruta `monolito` |
| T-08 | Quitar springdoc y borrar `docs/openapi/cash.json` |
| Cualquier punto | `DROP DATABASE gestion_cash` y reaplicar; el monolito conserva `sistema_gestion` |

## 11. Entregables

```text
microservices/cash-service/src/main/java/com/cibertec/gestion/cash/
├─ CashServiceApplication.java
├─ clients/{CatalogoClient.java,dto/{PuestoRemoto,SocioRemoto,ServicioRemoto,BancoRemoto}.java}
├─ config/OpenApiConfig.java
├─ controllers/{CuentaCobrarController,ReciboController}.java
├─ dto/{CuentaRequest,GenerarCuentasRequest,OperacionLoteRequest,CanjeRequest,IngresoExternoRequest}.java
├─ entities/{CuentaCobrar,Recibo,Correlativo}Entity.java
├─ enums/{EstadoCuenta,TipoRecibo}.java
├─ exceptions/{ApiExceptionHandler,BusinessException,ConflictException,NotFoundException,DependenciaNoDisponibleException}.java
├─ mappers/{CuentaCobrarMapper,ReciboMapper}.java
├─ repositories/{CuentaCobrar,Recibo,Correlativo}Repository.java
├─ security/{JwtService,JwtAuthenticationFilter,WebSecurityConfig,UsuarioPrincipal}.java
└─ services/{ICuentaCobrarService,CuentaCobrarService,IReciboService,ReciboService,CorrelativoService}.java
microservices/cash-service/src/test/java/.../CuentaCobrarServiceTest.java
docs/openapi/cash.json
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json (carpetas 03 y 04)
```

## 12. Fuera de alcance

- Egresos, importación CSV y anulación (PLAN-05).
- Reportes XLSX (PLAN-05).
- Reintentos, circuit breaker o caché de catálogo.
- Sincronización con la base del monolito.
- CORS.
- Modificación de `database/flyway/cash/**` o de `backend/`.
