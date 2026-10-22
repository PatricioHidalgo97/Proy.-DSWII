# ESP-13 — Convenciones y definición de terminado

## 1. Nomenclatura

### 1.1 Proyectos y módulos

| Elemento | Convención | Ejemplo |
|---|---|---|
| Carpeta de servicio | `kebab-case` | `identity-service` |
| `artifactId` | `kebab-case` | `cash-service` |
| Paquete base | `com.cibertec.gestion` | `com.cibertec.gestion.cash` |
| Clase de arranque | `{Servicio}Application` | `CashServiceApplication` |
| Perfil | `application-{entorno}.properties` | `application-demo.properties` |

### 1.2 Código Java

| Elemento | Convención |
|---|---|
| Entidades | `XxxEntity` |
| Repositorios | `XxxRepository` |
| Servicios | `IXxxService` + `XxxService` |
| Controladores | `XxxController` |
| Request | `XxxRequest` |
| Response | `XxxResponse` |
| Mapper | `XxxMapper` |
| Enums | `PascalCase` con valores `UPPER_SNAKE` |
| Excepciones | `XxxException` |
| Pruebas | `XxxTest` |

### 1.3 Base de datos

| Elemento | Convención |
|---|---|
| Tablas | `snake_case` en plural | `cuentas_cobrar` |
| Columnas | `snake_case` | `fecha_creacion` |
| Claves foráneas | `fk_{tabla}_{referencia}` | `fk_puestos_socio` |
| Restricciones únicas | `uk_{tabla}_{columna}` | `uk_socios_codigo` |
| Índices | `ix_{tabla}_{columna}` | `ix_cuentas_estado` |
| Migraciones | `V{n}__{descripcion}.sql` |

### 1.4 API

| Elemento | Convención |
|---|---|
| Rutas | plural, minúsculas | `/api/cuentas` |
| JSON | `camelCase` | `fechaCreacion` |
| Fechas | ISO 8601 | `2026-10-21`, `2026-10-21T10:15:30` |
| Enums | `UPPER_SNAKE` | `PENDIENTE` |
| Booleanos | `true` / `false` |

### 1.5 Documentación

| Elemento | Convención |
|---|---|
| Especificaciones | `NN-titulo-en-kebab-case.md` |
| Planes | `NN-titulo-en-kebab-case.md` |
| IDs de especificación | `ESP-NN` |
| IDs de decisión | `PR-NN`, `R-NN`, `PORT-XXX` |
| Referencias | `Ver ESP-03, sección 4.2` |

## 2. Estructura de un microservicio

```text
{servicio}/
├─ .env.example
├─ pom.xml
├─ README.md
└─ src/
   ├─ main/
   │  ├─ java/com/cibertec/gestion/{dominio}/
   │  │  ├─ config/
   │  │  ├─ controllers/
   │  │  ├─ dto/
   │  │  ├─ entities/
   │  │  ├─ enums/
   │  │  ├─ exceptions/
   │  │  ├─ mappers/
   │  │  ├─ repositories/
   │  │  ├─ security/
   │  │  └─ services/
   │  └─ resources/
   │     └─ application.properties
   └─ test/java/com/cibertec/gestion/{dominio}/
```

## 3. Estilo de código

| Regla | Detalle |
|---|---|
| Inyección | Por constructor (`@RequiredArgsConstructor`) |
| Setters | Solo donde el framework los exige |
| DTOs | Inmutables cuando sea posible (`record` o `@Getter`) |
| Mapeo | Centralizado en `mappers` |
| Transacciones | En la capa de servicio, no en controladores |
| Validación | Bean Validation en los request |
| Excepciones | De dominio, traducidas por el handler |
| Logs | Sin datos personales ni credenciales |
| Comentarios | Explican el porqué, no el qué |

## 4. Reglas de contrato

1. Un endpoint no se renombra.
2. Un campo no se renombra ni se elimina.
3. Un campo nuevo se agrega sin romper a los consumidores.
4. Los `null` se mantienen explícitos.
5. Los códigos de error no cambian de significado.
6. Todo cambio se refleja en OpenAPI y en Postman.

## 5. Pruebas

| Nivel | Herramienta | Alcance |
|---|---|---|
| Unidad | JUnit + Mockito | Reglas de negocio aisladas |
| Integración | Spring Boot Test | Repositorios y controladores |
| Contrato | Postman/Newman | Flujo completo por gateway |
| Manual | Swagger UI | Endpoint individual |

### 5.1 Regla de pruebas

- Toda regla de negocio migrada conserva al menos una prueba equivalente.
- Las pruebas de caja mockean el cliente de catálogo.
- Ninguna prueba depende del orden de ejecución.

## 6. Definición de terminado

Una tarea está terminada cuando:

- [ ] Compila sin advertencias nuevas.
- [ ] Las pruebas existentes pasan.
- [ ] El contrato no cambió (o se documentó el cambio).
- [ ] OpenAPI actualizado.
- [ ] Postman actualizado y pasando.
- [ ] Sin credenciales en el código.
- [ ] Puertos configurables.
- [ ] Documentación al día.
- [ ] El monolito sigue arrancando.

Un servicio está terminado cuando:

- [ ] Todas sus tareas cumplen la definición anterior.
- [ ] Su migración Flyway aplica desde cero.
- [ ] Su Swagger funciona.
- [ ] Su health check responde.
- [ ] Su contrato está exportado.

La migración está terminada cuando:

- [ ] El frontend funciona solo por el gateway.
- [ ] No hay llamadas al monolito.
- [ ] Los tres servicios arrancan de forma independiente.
- [ ] La colección completa pasa.
- [ ] La demostración se ejecuta sin pasos manuales de datos.

## 7. Manejo de decisiones nuevas

1. Toda decisión nueva se registra en la especificación correspondiente.
2. Si afecta el contrato, se actualiza ESP-04.
3. Si afecta puertos o variables, se actualiza ESP-02.
4. Si afecta datos, se actualiza ESP-03 y ESP-06.
5. Si es transversal, se agrega una nueva ESP y se indexa en el README.
6. Los planes de implementación citan el ID de la decisión.

## 8. Idioma

| Elemento | Idioma |
|---|---|
| Documentación | Español |
| Mensajes de error de API | Español |
| Nombres de clases y métodos | Inglés técnico |
| Nombres de tablas y columnas | Inglés técnico en `snake_case` |
| Commits | Español, formato convencional |

## 9. Control de versiones

| Rama | Uso |
|---|---|
| `main` | Versión estable |
| `feature/microservicios-api-gateway` | Migración en curso |

Reglas:

- Un commit por unidad lógica.
- Mensajes en formato `tipo(alcance): descripción`.
- No se versionan `.env`, `node_modules`, `target` ni `data`.
- Las migraciones aplicadas no se reescriben.
