# Ejecución PLAN-03 — catalog-service

Evaluación independiente del orquestador. El ejecutor entregó informe completo y
abrió el PR; todo lo de abajo está verificado contra el repositorio y el sistema
en ejecución, no contra el informe.

## Rama y commits

Rama `feature/catalog-service`, PR **#8** (OPEN, MERGEABLE).

| Commit | Mensaje |
|---|---|
| `482293d9` | feat(catalog): agregar entidades, enums y repositorios de catalogo |
| `b68f17f1` | feat(catalog): implementar CRUD de socios, giros, puestos, servicios y bancos |
| `ad7b123c` | feat(catalog): agregar springdoc, pruebas de catalogo y configuracion OpenAPI |
| `0e88fe8f` | feat(gateway): enrutar los cinco prefijos de catalogo a catalog-service |
| `7b40525d` | fix(catalog): mapear cargoA a la columna cargo_a del esquema Flyway |
| `1becf916` | docs(openapi): exportar el contrato de catalog-service |
| `d7b391fd` | test(postman): agregar la carpeta 02 · Catalogos |

## Criterios de aceptación

| CA | Estado | Evidencia literal |
|---|---|---|
| CA-01 | Cumple | Newman: `requests 41 / failed 0`, `assertions 81 / failed 0` |
| CA-02 | Cumple | `GET /api/puestos/1` con `socio` (8 campos) y `giro` (4 campos) anidados |
| CA-03 | Cumple | `GET /api/puestos/5` → `"socio":null` **presente**, `giro` con datos |
| CA-04 | Cumple | token emitido por 9102 → `GET http://localhost:9103/api/socios` → `200` |
| CA-05 | Cumple | operador: GET 200; POST 403; PUT 403; DELETE 403 |
| CA-06 | Cumple | socios, giros, puestos, servicios y bancos duplicados → `409` con los mensajes del monolito (p. ej. `"Ya existe la cuenta bancaria indicada"`) |
| CA-07 | Cumple | `pom.xml` sin `resttemplate/webclient/restclient/feign/httpclient`; `src/main` sin `localhost:9102`, `localhost:9104` ni `java.net.http` |
| CA-08 | Cumple | con el monolito **detenido** (`netstat` sin 9090): socios/giros/puestos/servicios/bancos → `200`; `/api/cuentas` → `500` |
| CA-09 | Cumple | `docs/openapi/catalog.json`: `paths: 10`, `operaciones: 25`, `tags: Bancos,Giros,Puestos,Servicios,Socios`, `schemes: ['bearerAuth']` |
| CA-10 | Cumple | `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| CA-11 | Cumple (con salvedad) | `/api/cuentas` por gateway con monolito arriba → `200`; con monolito abajo → `500`, lo que demuestra que se sirve desde 9090. Devuelve `[]` porque `sistema_gestion` no tiene filas de caja: los seeds viven en `gestion_cash`, que aún no sirve ningún servicio |
| CA-12 | Cumple | `GET http://localhost:9103/actuator/health` → `{"status":"UP"}` |

## Verificaciones propias destacadas

- **Migraciones intactas**: `git diff --name-only main..HEAD -- database/ backend/` → vacío.
- **Health público**: `WebSecurityConfig` línea 35 incluye `.requestMatchers("/actuator/health").permitAll()` — la alineación de PLAN-08 funcionó.
- **Datos de seed intactos** tras la suite: socios 24, giros 8, puestos 26, servicios 12, bancos 5. La subcarpeta `Limpieza` de Postman deja los datos como estaban.

## Decisiones aceptadas

### D-01 — Las rutas de catálogo cambian de URI en lugar de excluir prefijos de `monolito`

Los predicados `Path` en `application.properties` no admiten negación simple. Con
`order` 20–24 para los prefijos de catálogo y `order` 100 para `monolito`, los
específicos ganan. Verificado con el monolito apagado: catálogo sigue en 200 y
caja cae a 500. Es la resolución correcta y no cambia la arquitectura.

### D-02 — `@Column(name = "cargo_a")` en `ServicioEntity`

Hibernate derivaba `cargoa` y `ddl-auto=validate` fallaba contra el esquema
Flyway, que declara `cargo_a`. La alternativa era editar la migración, prohibido
por §5.2 del contrato y por el propio plan. Mapear la columna en la entidad es la
corrección correcta; la migración no se tocó (comprobado con `git diff`).

### D-03 — DTO `PuestoResponse` en lugar de serializar la entidad

Necesario para garantizar que `socio` se serialice como `null` explícito y no se
omita, que es exactamente lo que exige ESP-04 §7.5 y verifica CA-03.

### D-04 — H2 en scope `test` y `src/test/resources/application.properties`

Permite que las 10 pruebas corran sin MySQL y sin tocar la configuración de
producción. Mismo patrón que el monolito.

## Observaciones para planes posteriores

- La ruta `monolito` (`Path=/api/**`, order 100) sigue activa y debe retirarse en
  PLAN-05. Mientras exista, depende de que los prefijos específicos conserven
  `order` menor.
- `catalog-service` exige `JWT_SECRET` de al menos 32 caracteres, igual que
  identity: el mismo valor debe arrancar ambos.
- CA-11 quedará plenamente demostrable cuando cash-service sirva `/api/cuentas`
  desde `gestion_cash` (PLAN-04).
