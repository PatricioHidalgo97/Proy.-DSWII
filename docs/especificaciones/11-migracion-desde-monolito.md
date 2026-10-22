# ESP-11 — Migración desde el monolito

## 1. Decisión

Se aplica el patrón **strangler**: el monolito sigue funcionando mientras los
servicios se extraen uno por uno, y el gateway enruta cada prefijo al lugar
correcto.

| Principio | Consecuencia |
|---|---|
| El monolito nunca se rompe | Cada fase deja el sistema funcional |
| El contrato no cambia | El frontend migra una sola vez |
| Extracción por dominio | No por entidad ni por capa |
| Respaldo permanente | `backend/` y `crear_base.sql` quedan intactos |

## 2. Estados del sistema

### 2.1 Estado 0 — Monolito puro

```text
Angular 4200 -> Monolito 9090 -> sistema_gestion
```

Es el estado actual y debe seguir siendo reproducible.

### 2.2 Estado 1 — Gateway + identidad

```text
Angular 4400 -> Gateway 9101 -> identity 9102  (/api/auth/**)
                            -> Monolito 9090  (todo lo demás)
```

Beneficio: se demuestra el gateway y la seguridad sin tocar catálogo ni caja.

### 2.3 Estado 2 — Gateway + identidad + catálogo

```text
Angular 4400 -> Gateway 9101 -> identity 9102  (/api/auth/**)
                            -> catalog  9103  (catálogos)
                            -> Monolito 9090  (caja)
```

Beneficio: catálogo ya es independiente. Caja todavía usa la base del monolito.

### 2.4 Estado 3 — Arquitectura final

```text
Angular 4400 -> Gateway 9101 -> identity 9102
                            -> catalog  9103
                            -> cash     9104
```

El monolito queda como respaldo, sin recibir tráfico del frontend.

## 3. Fases de trabajo

| Fase | Alcance | Resultado verificable |
|---|---|---|
| F0 | Esqueleto, contratos y bases | Migraciones aplican; documentos listos |
| F1 | Gateway + identidad | Login funciona por el gateway |
| F2 | Catálogo | CRUD de catálogos por el gateway |
| F3 | Caja | Flujo completo de cobranza y tesorería |
| F4 | Cierre | Swagger, Postman, pruebas y demostración |

Las fases no tienen fechas: se consideran terminadas cuando cumplen sus
criterios de aceptación.

## 4. Qué se mueve en cada fase

### 4.1 Fase 1 — Identidad

| Se mueve | Origen |
|---|---|
| `UsuarioEntity` | `entities` |
| `Rol` | `enums` |
| `UsuarioRepository` | `repositories` |
| `UsuarioService` | `services/implementations` |
| `AuthController` | `controllers` |
| `JwtService`, `JwtAuthenticationFilter`, `WebSecurityConfig` | `security` |
| `LoginRequest`, `LoginResponse`, `UsuarioResponse` | `models` |
| `DataInitializer` (solo usuarios) | `config` |

No se mueve la siembra de correlativos: pertenece a caja.

### 4.2 Fase 2 — Catálogo

| Se mueve | Origen |
|---|---|
| `SocioEntity`, `GiroEntity`, `PuestoEntity`, `ServicioEntity`, `BancoEntity` | `entities` |
| Sus repositorios | `repositories` |
| Sus servicios | `services/implementations` |
| Sus controladores | `controllers` |
| Sus DTO de request | `models` |

Las relaciones `Puesto → Socio/Giro` se mantienen con JPA y claves foráneas
locales.

### 4.3 Fase 3 — Caja

| Se mueve | Origen |
|---|---|
| `CuentaCobrarEntity`, `ReciboEntity`, `EgresoEntity`, `CorrelativoEntity` | `entities` |
| Sus repositorios | `repositories` |
| `CuentaCobrarService`, `ReciboService`, `EgresoService`, `ReporteService`, `CorrelativoService` | `services/implementations` |
| Sus controladores | `controllers` |
| `ApiExceptionHandler`, excepciones | `exceptions` |
| `DataInitializer` (correlativos) | `config` |

### 4.4 Transformaciones en caja

| Antes | Después |
|---|---|
| `@ManyToOne` a `PuestoEntity` | `puestoId` + columnas snapshot |
| `@ManyToOne` a `SocioEntity` | `socioId` + columnas snapshot |
| `@ManyToOne` a `ServicioEntity` | `servicioId` + columnas snapshot |
| `@ManyToOne` a `BancoEntity` | `bancoId` + columnas snapshot |
| `@ManyToOne` a `UsuarioEntity` | `usuarioId` + columnas snapshot |
| Repositorios de catálogo inyectados | `CatalogoClient` |
| `UsuarioRepository` inyectado | `UsuarioPrincipal` del JWT |
| Entidades serializadas directo | DTO de respuesta + mapper |

## 5. Reglas de migración

1. **No se reescribe lógica.** Los métodos de cálculo, validación y transición
   de estado se copian tal cual.
2. **No se renombran endpoints.** El prefijo permanece.
3. **No se cambia el formato de respuesta.** Solo cambia la fuente del dato.
4. **Cada fase es reversible.** Si falla, el gateway puede volver a enrutar al
   monolito.
5. **El monolito no se modifica.** No se le quitan clases ni se le cambia la
   configuración.

## 6. Conviviencia durante la transición

| Recurso | Monolito | Microservicios |
|---|---|---|
| Base de datos | `sistema_gestion` | Tres esquemas |
| Puerto | 9090 | 9101–9104 |
| Frontend | Puede apuntar al monolito | Apunta al gateway |
| Usuarios | Se siembran ambos | Migración de identidad |
| Datos | Se mantienen | Seeds independientes |

**No hay sincronización de datos entre ambos.** Son entornos paralelos: el
monolito se usa como respaldo y demostración de la versión anterior.

## 7. Criterios de salida por fase

### Fase 0

- Tres bases creadas.
- Migraciones aplican sin error.
- Contratos documentados.
- Puertos verificados como libres.

### Fase 1

- Login por gateway devuelve token.
- `GET /api/auth/me` funciona.
- Un 401 y un 403 se comportan igual que en el monolito.
- El resto de rutas sigue respondiendo por el monolito.

### Fase 2

- CRUD de los cinco catálogos por gateway.
- Las escrituras exigen `ADMIN`.
- Los objetos anidados de puesto conservan `socio` y `giro`.
- Caja sigue funcionando contra el monolito.

### Fase 3

- Flujo completo por gateway.
- Cuentas conservan objetos anidados.
- Pagos generan recibos correlativos.
- Canjes e ingresos externos funcionan.
- Egresos y reportes funcionan.
- No queda ninguna llamada del frontend al monolito.

### Fase 4

- Swagger por servicio.
- OpenAPI exportado.
- Colección Postman completa y ejecutable.
- Documentación actualizada.
- Demostración ensayada.

## 8. Plan de reversa

| Falla en | Acción |
|---|---|
| Fase 1 | Gateway enruta `/api/auth/**` al monolito |
| Fase 2 | Gateway enruta catálogos al monolito |
| Fase 3 | Gateway enruta caja al monolito |
| Cualquier fase | El frontend vuelve a `http://localhost:9090/api` |

La reversa es de configuración, no de código.

## 9. Qué se conserva del monolito

| Elemento | Ubicación | Motivo |
|---|---|---|
| Código completo | `backend/` | Respaldo y comparación |
| Script de base | `database/crear_base.sql` | Reproducir la versión anterior |
| Perfil demo H2 | `application-demo.properties` | Demostración sin MySQL |
| Pruebas | `backend/src/test` | Referencia de comportamiento |
| Colección Postman original | `docs/` | Comparación de contrato |

## 10. Checklist de migración

- [ ] `crear_base.sql` intacto.
- [ ] `backend/` intacto.
- [ ] Frontend migrado al gateway.
- [ ] Cada fase deja el sistema funcional.
- [ ] Cada fase tiene plan de reversa.
- [ ] Sin sincronización de datos entre monolito y microservicios.
- [ ] Contrato verificado después de cada fase.
