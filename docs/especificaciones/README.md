# Especificaciones de la migración a microservicios

Esta carpeta reúne las decisiones tomadas para transformar el proyecto actual
—un monolito Spring Boot con frontend Angular— en una arquitectura de
**tres microservicios y un API Gateway**, sin alterar la lógica de negocio ni
el contrato que consume el frontend.

Cada documento es normativo: describe una decisión ya tomada, no una propuesta
abierta. Si una decisión debe cambiar, primero se actualiza la especificación y
después se ajustan los planes de implementación.

## Índice

| ID | Documento | Contenido |
|---|---|---|
| ESP-00 | [Visión, alcance y objetivos](00-vision-alcance-objetivos.md) | Qué se busca, qué entra y qué no |
| ESP-01 | [Arquitectura de microservicios](01-arquitectura-microservicios.md) | Servicios, límites y por qué ese corte |
| ESP-02 | [Puertos y entornos](02-puertos-y-entornos.md) | Mapa de puertos, variables y conflictos evitados |
| ESP-03 | [Fronteras y propiedad de datos](03-fronteras-y-propiedad-de-datos.md) | Quién es dueño de qué y cómo se comparte |
| ESP-04 | [Contrato de API pública](04-contrato-api-publica.md) | Endpoints, JSON y códigos de estado |
| ESP-05 | [Seguridad y JWT](05-seguridad-y-jwt.md) | Claims, secreto compartido, roles y CORS |
| ESP-06 | [Persistencia, Flyway y seeds](06-persistencia-flyway-seeds.md) | Tres esquemas, migraciones y datos de demostración |
| ESP-07 | [API Gateway](07-api-gateway.md) | Rutas, CORS, Actuator y límites |
| ESP-08 | [OpenAPI y Swagger](08-openapi-swagger.md) | springdoc, seguridad y exportación |
| ESP-09 | [Postman y pruebas](09-postman-y-pruebas.md) | Colección, entornos y Newman |
| ESP-10 | [Frontend y consumo](10-frontend-y-consumo.md) | Único cambio permitido en Angular |
| ESP-11 | [Migración desde el monolito](11-migracion-desde-monolito.md) | Estrategia strangler y respaldo |
| ESP-12 | [Riesgos y mitigaciones](12-riesgos-mitigaciones.md) | Riesgos identificados y su control |
| ESP-13 | [Convenciones y definición de terminado](13-convenciones-definicion-de-terminado.md) | Nomenclatura y criterios de cierre |

## Reglas de lectura

1. **ESP-01 y ESP-03 son el corazón del diseño.** El resto las desarrolla.
2. Todo plan de implementación debe citar los IDs que aplica. El contrato para
   escribir planes está en [`../plans/README.md`](../plans/README.md).
3. Los números concretos (puertos, versiones, nombres de bases) viven en
   ESP-02 y ESP-06. No se repiten en los planes: se referencian.
4. Nada de lo aquí escrito modifica el alcance académico del proyecto: no hay
   dinero real, no hay pasarela de pagos y el módulo bancario es simulado.

## Estado del repositorio

| Elemento | Ubicación | Estado |
|---|---|---|
| Monolito funcional | `backend/` | Se conserva como respaldo |
| Script legado | `database/crear_base.sql` | Se conserva sin cambios |
| Bases de microservicios | `database/crear_base_microservicios.sql` | Creado |
| Migraciones y seeds | `database/flyway/{auth,catalog,cash}/` | Creado y validado |
| Frontend | `frontend/` | Sin cambios de lógica |
