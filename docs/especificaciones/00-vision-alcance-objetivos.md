# ESP-00 — Visión, alcance y objetivos

## 1. Contexto

El proyecto nació como un monolito académico: **Sistema de Gestión
Administrativa y de Caja** para una asociación de comerciantes. Resuelve el
ciclo completo de un mercado o centro comercial:

catálogos → generación de deudas → cobranza → caja → reportes.

El profesor solicitó llevar ese mismo sistema a una arquitectura de
microservicios con API Gateway.

## 2. Objetivo

Dividir el sistema en **tres microservicios independientes** detrás de un
**API Gateway**, manteniendo:

- La misma lógica de negocio y las mismas reglas de validación.
- El mismo contrato HTTP que consume el frontend Angular.
- El mismo alcance académico: sin pasarela de pagos real ni movimiento de
  dinero.

## 3. Principios rectores

| ID | Principio | Consecuencia práctica |
|---|---|---|
| PR-01 | No cambiar la lógica | Las reglas de negocio se mueven de paquete, no se reescriben |
| PR-02 | No romper el contrato | El JSON público conserva nombres, anidamientos y códigos |
| PR-03 | Un dueño por dato | Cada tabla tiene un único servicio responsable de escribirla |
| PR-04 | Fronteras por dominio, no por entidad | Los servicios agrupan casos de uso completos |
| PR-05 | Sin transacciones distribuidas | Las llamadas entre servicios son lecturas previas, nunca escrituras cruzadas |
| PR-06 | Simplicidad operativa | Sin Eureka, sin mensajería, sin orquestador |
| PR-07 | Configuración externa | Puertos, credenciales y URLs se definen por entorno |
| PR-08 | Demostrable | El sistema debe poder levantarse y presentarse con datos reales de ejemplo |

## 4. Alcance

### Incluido

- Tres microservicios: identidad, catálogo y caja.
- Un API Gateway que centraliza el acceso del frontend.
- Una base de datos por microservicio, con migraciones versionadas.
- Datos de demostración suficientes para una presentación.
- Documentación OpenAPI/Swagger por servicio.
- Colección Postman con entornos y pruebas automatizables.
- Especificaciones y planes de implementación.

### Excluido

- Descubrimiento de servicios (Eureka u otro).
- Mensajería asíncrona (Kafka, RabbitMQ).
- Transacciones distribuidas, sagas u outbox.
- Contenedores y orquestación.
- Autenticación OAuth2/OpenID Connect con proveedor externo.
- Pasarela de pagos o conciliación bancaria real.
- Reescritura del frontend.

## 5. Criterios de éxito

1. El frontend funciona sin cambios de lógica, apuntando al gateway.
2. Cada endpoint existente responde con la misma forma de JSON.
3. Cada base de datos se crea y se puebla desde cero con Flyway.
4. Un usuario `ADMIN` y un usuario `OPERADOR` conservan exactamente sus
   permisos actuales.
5. El flujo completo de demostración (login → catálogos → deuda → pago →
   recibo → tesorería → reporte) se ejecuta a través del gateway.
6. La colección Postman recorre ese flujo y pasa sus pruebas.
7. El monolito original sigue siendo ejecutable como respaldo.

## 6. Glosario

| Término | Significado |
|---|---|
| Monolito | Aplicación actual en `backend/`, con una sola base |
| Gateway | Punto de entrada único HTTP para el frontend |
| Snapshot | Copia de datos de otro servicio guardada localmente al emitir un documento |
| Correlativo | Secuencia transaccional de numeración de recibos y egresos |
| Seed | Datos iniciales cargados por una migración |
| Contrato | Forma pública de una petición o respuesta HTTP |
