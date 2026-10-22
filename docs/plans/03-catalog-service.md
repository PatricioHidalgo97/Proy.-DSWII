# PLAN-03 — catalog-service

## 1. Objetivo

Extraer socios, giros, puestos, servicios y bancos a un servicio independiente
sobre `gestion_catalog`, que exponga los 25 endpoints de ESP-04 §4.2 conservando
los objetos anidados `socio` y `giro` dentro de `puesto`, y que sirva de fuente
de lectura para caja.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-01 | §1, §3.2, §4 | Catálogo conserva cinco entidades; nadie lo llama hacia atrás |
| ESP-03 | §1, §2, §3, §4.2, §4.3, §4.4, §4.5, §6, §7 | Propiedad de datos y lecturas que caja hará |
| ESP-04 | §4.2, §5.3, §6, §7, §8 | Contrato y anidamiento de `puesto` |
| ESP-05 | §3, §4, §5, §11 | Validación de JWT y reglas ADMIN |
| ESP-06 | §1, §2, §3 | `gestion_catalog`, Flyway `catalog` |
| ESP-08 | §4, §5, §7.2, §8 | Swagger de catálogo |
| ESP-11 | §3 (F2), §4.2, §7 (Fase 2), §8 | Qué se mueve y criterios de salida |
| ESP-12 | R-01, R-04, R-12 | Riesgos aplicables |
| ESP-13 | §1.2, §3, §5.1 | Nomenclatura, estilo y pruebas |

## 3. Alcance

### Dentro

- Las cinco entidades con sus relaciones JPA locales.
- Los cinco servicios con sus reglas de duplicidad y validación.
- Los cinco controladores con el patrón CRUD de ESP-04 §4.2.
- Filtro JWT y cadena de seguridad con escrituras solo `ADMIN`.
- Manejo de errores con la forma de ESP-04 §6.
- springdoc con cinco tags y exportación de `docs/openapi/catalog.json`.
- Pruebas equivalentes a `SocioRepositoryTest` y a las reglas de duplicidad.
- Carpeta `02 · Catálogos` de Postman.
- Retiro de los cinco prefijos de la ruta `monolito` en el gateway.

### Fuera

- Endpoints que caja necesita y que ya existen aquí: no se crean endpoints nuevos.
- Cualquier llamada de catálogo hacia caja o identidad.
- Reportes de socios y bancos (PLAN-05).
- Cambios en `database/flyway/catalog/**`.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-00 y PLAN-01 cerrados | Índice de `docs/plans/README.md` |
| `gestion_catalog` con sus 4 migraciones aplicadas | `flyway_schema_history` |
| `JWT_SECRET` idéntico al de identidad | Login en 9102 y llamada a 9103 |
| Los 26 puestos, 24 socios, 8 giros, 12 servicios y 5 bancos sembrados | Conteos de PLAN-00 §9.1 |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-09 compara `puesto` anidado con ESP-04 §5.3; T-10 lo verifica en Postman |
| R-04 Secretos distintos | Alto | T-05 valida el mismo token que emitió identidad |
| R-12 Contratos divergentes | Medio | T-07 exporta el OpenAPI en el mismo plan que crea los endpoints |

## 6. Tareas

### T-01 — Crear entidades, enums y repositorios

- **Objetivo:** mapear las cinco tablas con sus relaciones.
- **Especificación:** ESP-03 §3; ESP-06 §1; ESP-13 §1.2.
- **Archivos:**
  - `.../catalog/entities/{Socio,Giro,Puesto,Servicio,Banco}Entity.java`
  - `.../catalog/enums/{CargoA,Moneda,Recurrencia}.java`
  - `.../catalog/repositories/{Socio,Giro,Puesto,Servicio,Banco}Repository.java`
- **Depende de:** PLAN-00.
- **Pasos:**
  1. Copiar las cinco entidades del monolito cambiando solo el paquete.
  2. Conservar `@ManyToOne` de `Puesto` hacia `Socio` y `Giro`: son claves foráneas locales.
  3. Copiar los tres enums y los cinco repositorios sin cambios.
  4. No agregar `UsuarioEntity` ni nada de caja.
- **Verificación:** `ddl-auto=validate` no falla y `puestoRepository.findAll()` trae 26 filas.
- **Terminado cuando:** Hibernate valida las cinco tablas.

### T-02 — Implementar socios y giros

- **Objetivo:** CRUD con las reglas de duplicidad actuales.
- **Especificación:** ESP-04 §4.2; ESP-13 §3.
- **Archivos:**
  - `.../catalog/services/{ISocioService,SocioService,IGiroService,GiroService}.java`
  - `.../catalog/controllers/{SocioController,GiroController}.java`
  - `.../catalog/dto/{SocioRequest,GiroRequest}.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar `SocioService` y `GiroService` sin cambiar sus reglas.
  2. Conservar los `ConflictException` con los mismos mensajes.
  3. Copiar los request con sus anotaciones de validación.
  4. Implementar los cinco verbos por recurso.
- **Verificación:** crear un socio duplicado devuelve 409 con el mensaje del monolito.
- **Terminado cuando:** los dos CRUD responden igual que hoy.

### T-03 — Implementar puestos con anidamiento

- **Objetivo:** que `puesto` siga incluyendo `socio` y `giro` completos.
- **Especificación:** ESP-04 §5.3; ESP-03 §3.
- **Archivos:**
  - `.../catalog/services/{IPuestoService,PuestoService}.java`
  - `.../catalog/controllers/PuestoController.java`
  - `.../catalog/dto/{PuestoRequest,PuestoResponse}.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar `PuestoService` con la validación de fechas y la resolución de `socioId` y `giroId`.
  2. Definir `PuestoResponse` con `id`, `numero`, `nombreInquilino`, `fechaInicio`, `fechaFin`, `socio`, `giro`, `activo`.
  3. Anidar `socio` con los ocho campos de ESP-04 §5.3 y `giro` con `id`, `nombre`, `descripcion`, `activo`.
  4. Mantener `socio` como `null` cuando el puesto no lo tiene.
  5. Implementar los cinco verbos.
- **Verificación:** `GET /api/puestos/1` devuelve `socio` y `giro` no nulos, y `GET /api/puestos/5` devuelve `socio` nulo con `giro` presente.
- **Terminado cuando:** ambas respuestas coinciden con ESP-04 §5.3.

### T-04 — Implementar servicios y bancos

- **Objetivo:** completar los cinco catálogos.
- **Especificación:** ESP-04 §4.2.
- **Archivos:**
  - `.../catalog/services/{IServicioService,ServicioService,IBancoService,BancoService}.java`
  - `.../catalog/controllers/{ServicioController,BancoController}.java`
  - `.../catalog/dto/{ServicioRequest,BancoRequest}.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar ambos servicios sin alterar la validación de `numeroCuenta` y `cci`.
  2. Conservar los tres enums en las respuestas.
  3. Implementar los cinco verbos por recurso.
- **Verificación:** crear un banco con CCI duplicado devuelve 409.
- **Terminado cuando:** ambos CRUD replican el comportamiento actual.

### T-05 — Configurar seguridad y errores

- **Objetivo:** validar JWT y restringir escrituras a `ADMIN`.
- **Especificación:** ESP-05 §3, §4, §5, §11.
- **Archivos:**
  - `.../catalog/security/{JwtService,JwtAuthenticationFilter,WebSecurityConfig,UsuarioPrincipal}.java`
  - `.../catalog/exceptions/{ApiExceptionHandler,BusinessException,ConflictException,NotFoundException}.java`
- **Depende de:** T-01.
- **Pasos:**
  1. Copiar el `JwtService` y el filtro de identidad, con `UsuarioPrincipal`.
  2. Reglas: `OPTIONS /**` y Swagger públicos; `DELETE /api/**` ADMIN; `POST` y `PUT` de los cinco recursos ADMIN; resto autenticado.
  3. Sin CORS.
  4. Copiar el `ApiExceptionHandler` completo.
  5. Validar al arranque que el secreto tenga al menos 32 caracteres.
- **Verificación:** un token de operador recibe 403 al crear un socio y 200 al listarlos.
- **Terminado cuando:** las reglas de ESP-05 §5 se cumplen para catálogo.

### T-06 — Actualizar el gateway

- **Objetivo:** que los cinco prefijos dejen de ir al monolito.
- **Especificación:** ESP-07 §4; ESP-11 §2.3.
- **Archivos:** `microservices/api-gateway/src/main/resources/application.properties`
- **Depende de:** T-02, T-03, T-04.
- **Pasos:**
  1. Confirmar que la ruta `catalog` ya apunta a 9103.
  2. Excluir del `Path` de la ruta `monolito` los cinco prefijos de catálogo.
  3. Verificar que caja sigue llegando al monolito.
- **Verificación:** `GET /api/socios` va a 9103 y `GET /api/cuentas` sigue yendo a 9090.
- **Terminado cuando:** los prefijos de catálogo ya no tocan el monolito.

### T-07 — Agregar springdoc y exportar el contrato

- **Objetivo:** documentar los 25 endpoints.
- **Especificación:** ESP-08 §4, §5, §7.2, §8.
- **Archivos:**
  - `microservices/catalog-service/pom.xml`
  - `.../catalog/config/OpenApiConfig.java`
  - `docs/openapi/catalog.json`
- **Depende de:** T-05.
- **Pasos:**
  1. Agregar springdoc 2.8.14.
  2. Declarar `@OpenAPIDefinition` y `@SecurityScheme(bearerAuth)`.
  3. Anotar cada controlador con su `@Tag`: Socios, Giros, Puestos, Servicios, Bancos.
  4. Documentar códigos 200, 201, 204, 400, 401, 403, 404, 409.
  5. Exportar a `docs/openapi/catalog.json`.
- **Verificación:** el JSON exportado contiene los 10 paths y las 25 operaciones con sus cinco tags.
- **Terminado cuando:** Swagger funciona y el contrato está exportado.

### T-08 — Portar las pruebas de catálogo

- **Objetivo:** conservar pruebas de repositorio y de reglas de duplicidad.
- **Especificación:** ESP-13 §5, §5.1.
- **Archivos:**
  - `src/test/java/.../catalog/repositories/SocioRepositoryTest.java`
  - `src/test/java/.../catalog/services/CatalogoValidacionTest.java`
- **Depende de:** T-02, T-04.
- **Pasos:**
  1. Copiar `SocioRepositoryTest` con sus cuatro casos.
  2. Agregar pruebas de duplicidad para giro, puesto, servicio y banco.
  3. Agregar una prueba de fecha de fin anterior a la de inicio.
- **Verificación:** `mvn test` en verde.
- **Terminado cuando:** las pruebas cubren las cuatro reglas de duplicidad y la de fechas.

### T-09 — Alinear el contrato con la línea base

- **Objetivo:** verificar el anidamiento y los códigos.
- **Especificación:** ESP-04 §5.3, §6, §7, §8.
- **Archivos:** ninguno.
- **Depende de:** T-03.
- **Pasos:**
  1. Comparar `puesto` con ESP-04 §5.3 campo por campo.
  2. Verificar que un puesto sin socio devuelve `"socio": null` y no omite el campo.
  3. Verificar los códigos 201, 204, 400, 409.
- **Verificación:** las tres comprobaciones coinciden.
- **Terminado cuando:** no hay diferencias con la línea base.

### T-10 — Completar la carpeta de catálogos en Postman

- **Objetivo:** cubrir los cinco CRUD con casos positivos y negativos.
- **Especificación:** ESP-09 §3, §4.2, §7.1.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-02 a T-04.
- **Pasos:**
  1. Crear socio, giro, puesto, servicio de puesto, servicio de socio y banco, guardando sus ids.
  2. Listar cada recurso y verificar que contiene el id creado.
  3. Repetir un alta y verificar 409.
  4. Actualizar y verificar 200; eliminar y verificar 204.
  5. Usar los sufijos `PM-` de ESP-09 §7.1.
- **Verificación:** la carpeta pasa completa en Newman.
- **Terminado cuando:** los cinco CRUD están cubiertos con asserts.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01 | Persistencia y seguridad base |
| 2 | T-02, T-03, T-04, T-05 | Los cuatro grupos de endpoints y la seguridad |
| 3 | T-06, T-07, T-08, T-09 | Gateway, Swagger, pruebas y contrato |
| 4 | T-10 | Postman, requiere T-06 y T-09 |

T-06 solo puede ejecutarse cuando los cinco CRUD funcionan: es el punto en que
catálogo deja de depender del monolito.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | Los 25 endpoints responden por el gateway | Newman de la carpeta `02 · Catálogos` en verde | ESP-04 §4.2 |
| CA-02 | `puesto` conserva `socio` y `giro` | Comparación con ESP-04 §5.3 | ESP-04 §5.3 |
| CA-03 | Un puesto sin socio devuelve `socio: null` explícito | `GET /api/puestos/5` | ESP-04 §7.5 |
| CA-04 | Un token de identidad es aceptado | Login en 9102 y `GET /api/socios` en 9103 | ESP-05 §7 |
| CA-05 | Las escrituras exigen ADMIN | Operador recibe 403 en POST, PUT y DELETE | ESP-05 §5 |
| CA-06 | Las cuatro reglas de duplicidad devuelven 409 | Prueba por recurso | ESP-04 §6 |
| CA-07 | Catálogo no consulta a caja ni a identidad | Sin dependencias HTTP en el POM ni en el código | ESP-01 §4 |
| CA-08 | Los cinco prefijos ya no pasan por el monolito | Detener el monolito y listar los cinco recursos | ESP-11 §7 Fase 2 |
| CA-09 | Swagger y contrato exportado | `docs/openapi/catalog.json` con 10 paths y 25 operaciones | ESP-08 §8 |
| CA-10 | Las pruebas pasan | `mvn test` en verde | ESP-13 §5.1 |
| CA-11 | Caja sigue funcionando contra el monolito | Las pantallas de caja cargan | ESP-11 §7 Fase 2 |

## 9. Verificación

```powershell
# JWT_SECRET se toma del entorno local; no se escribe en el plan (contrato de planes, seccion 4.2)
if (-not $env:JWT_SECRET) { throw 'Defina JWT_SECRET antes de arrancar los servicios' }
$token = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"admin\",\"password\":\"Admin123*\"}' | ConvertFrom-Json).token
$operador = (curl.exe -s -X POST http://localhost:9101/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usuario\":\"caja\",\"password\":\"Caja123*\"}' | ConvertFrom-Json).token

# CA-01 y CA-03
curl.exe -s http://localhost:9101/api/puestos/1 -H "Authorization: Bearer $token"
curl.exe -s http://localhost:9101/api/puestos/5 -H "Authorization: Bearer $token"

# CA-05
curl.exe -i -X POST http://localhost:9101/api/socios `
  -H "Authorization: Bearer $operador" -H "Content-Type: application/json" `
  -d '{\"codigo\":\"SOC-X\",\"nombres\":\"X\",\"apellidos\":\"Y\",\"etapa\":1,\"activo\":true}'

# CA-08
Stop-Process -Name java -ErrorAction SilentlyContinue   # monolito detenido
curl.exe -s http://localhost:9101/api/socios -H "Authorization: Bearer $token"

# CA-09
curl.exe http://localhost:9103/v3/api-docs -o docs/openapi/catalog.json

# CA-10
cd microservices/catalog-service ; mvn test
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 a T-05 | Detener catálogo; el gateway vuelve a enrutar los cinco prefijos al monolito |
| T-06 | Restaurar el `Path` de la ruta `monolito` a `/api/**` |
| T-07 | Quitar springdoc y borrar `docs/openapi/catalog.json` |
| Cualquier punto | `DROP DATABASE gestion_catalog` y reaplicar |

## 11. Entregables

```text
microservices/catalog-service/src/main/java/com/cibertec/gestion/catalog/
├─ CatalogServiceApplication.java
├─ config/OpenApiConfig.java
├─ controllers/{Socio,Giro,Puesto,Servicio,Banco}Controller.java
├─ dto/{SocioRequest,GiroRequest,PuestoRequest,PuestoResponse,ServicioRequest,BancoRequest}.java
├─ entities/{Socio,Giro,Puesto,Servicio,Banco}Entity.java
├─ enums/{CargoA,Moneda,Recurrencia}.java
├─ exceptions/{ApiExceptionHandler,BusinessException,ConflictException,NotFoundException}.java
├─ repositories/{Socio,Giro,Puesto,Servicio,Banco}Repository.java
├─ security/{JwtService,JwtAuthenticationFilter,WebSecurityConfig,UsuarioPrincipal}.java
└─ services/{ISocioService,SocioService,IGiroService,GiroService,IPuestoService,PuestoService,IServicioService,ServicioService,IBancoService,BancoService}.java
microservices/catalog-service/src/test/java/.../{SocioRepositoryTest,CatalogoValidacionTest}.java
docs/openapi/catalog.json
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json (carpeta 02)
```

## 12. Fuera de alcance

- Reportes de socios y bancos.
- Endpoints adicionales para caja.
- Consultas de catálogo hacia otros servicios.
- CORS.
- Modificación de `database/flyway/catalog/**` o de `backend/`.
