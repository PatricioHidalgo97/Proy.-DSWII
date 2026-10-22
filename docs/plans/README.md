# Contrato para la elaboración de planes de implementación

Este documento define **cómo deben escribirse los planes** que ejecutan las
decisiones de [`../especificaciones/`](../especificaciones/). No describe el
proyecto: describe la forma obligatoria de planificarlo.

Un plan no decide arquitectura. La arquitectura ya está decidida y vive en las
especificaciones. El plan responde una sola pregunta: **qué se hace, en qué
orden, con qué criterio de aceptación y cómo se verifica**.

---

## 1. Dónde viven los planes

```text
docs/
├─ especificaciones/     # decisiones (qué y por qué)
└─ plans/                # ejecución (cómo y en qué orden)
   ├─ README.md          # este contrato
   ├─ 00-...md
   ├─ 01-...md
   └─ ...
```

### 1.1 Reglas de ubicación

| Regla | Detalle |
|---|---|
| Un archivo por plan | No se agrupan fases distintas en un mismo archivo |
| Nombre | `NN-titulo-en-kebab-case.md` |
| Numeración | Consecutiva, sin huecos |
| Índice | Toda alta de plan se refleja en la tabla de la sección 9 |
| Idioma | Español |
| Formato | Markdown |

### 1.2 Separación de responsabilidades

| Carpeta | Contiene | No contiene |
|---|---|---|
| `especificaciones/` | Decisiones, reglas, contratos, riesgos | Tareas, pasos, checklists de ejecución |
| `plans/` | Tareas, orden, criterios de aceptación, verificación | Decisiones nuevas de arquitectura |

Si al escribir un plan aparece una decisión no documentada, **se detiene el
plan** y primero se actualiza la especificación correspondiente. El plan nunca
introduce arquitectura por su cuenta.

---

## 2. Entradas obligatorias de un plan

Antes de escribir una sola tarea, el plan debe declarar:

| Entrada | Obligatoria | Contenido |
|---|---|---|
| Especificaciones aplicables | Sí | Lista de IDs (`ESP-01`, `ESP-03`, …) |
| Alcance | Sí | Qué queda dentro y qué queda fuera de este plan |
| Precondiciones | Sí | Estado del repositorio, servicios y datos que deben existir |
| Riesgos aplicables | Sí | IDs de `ESP-12` con su mitigación concreta en este plan |
| Entregables | Sí | Archivos, artefactos y comportamientos observables |
| Criterios de aceptación | Sí | Condiciones verificables de cierre |
| Verificación | Sí | Cómo se comprueba cada criterio |
| Reversa | Sí | Cómo se deshace el plan si falla |

Un plan sin precondiciones o sin criterios de aceptación está incompleto.

---

## 3. Estructura obligatoria

Todo plan usa exactamente estas secciones, en este orden:

```markdown
# PLAN-NN — Título

## 1. Objetivo
## 2. Especificaciones aplicables
## 3. Alcance
## 4. Precondiciones
## 5. Riesgos y mitigaciones
## 6. Tareas
## 7. Orden de ejecución
## 8. Criterios de aceptación
## 9. Verificación
## 10. Reversa
## 11. Entregables
## 12. Fuera de alcance
```

### 3.1 Descripción de cada sección

| Sección | Qué debe contener |
|---|---|
| Objetivo | Una o dos frases sobre el resultado del plan |
| Especificaciones aplicables | Tabla ID → sección → qué se aplica |
| Alcance | Lista explícita de lo que entra y lo que no |
| Precondiciones | Estado verificable del entorno antes de empezar |
| Riesgos y mitigaciones | Riesgo, impacto, mitigación concreta dentro del plan |
| Tareas | Tareas atómicas, numeradas, con archivos afectados |
| Orden de ejecución | Secuencia y dependencias entre tareas |
| Criterios de aceptación | Condiciones binarias, verificables |
| Verificación | Comandos o pasos concretos por criterio |
| Reversa | Pasos para volver al estado anterior |
| Entregables | Artefactos producidos |
| Fuera de alcance | Lo que deliberadamente no se hace |

---

## 4. Reglas para redactar tareas

### 4.1 Formato de tarea

```markdown
### T-01 — Título corto

- **Objetivo:** resultado concreto de la tarea.
- **Especificación:** ESP-XX §Y.
- **Archivos:** rutas exactas que se crean o modifican.
- **Depende de:** T-00 (o "ninguna").
- **Pasos:**
  1. Acción concreta.
  2. Acción concreta.
- **Verificación:** cómo se comprueba que quedó bien.
- **Terminado cuando:** condición binaria.
```

### 4.2 Reglas

| Regla | Detalle |
|---|---|
| Atomicidad | Una tarea produce un resultado verificable |
| Tamaño | Si una tarea tiene más de siete pasos, se divide |
| Rutas | Siempre explícitas, nunca “el servicio” |
| Dependencias | Declaradas, nunca implícitas |
| Verificación | Obligatoria en cada tarea |
| Ambigüedad | Prohibida: “ajustar lo necesario” no es una tarea |
| Cambios de contrato | Se declaran y se reflejan en ESP-04 |
| Secretos | Nunca se escriben en el plan ni en el código |

### 4.3 Prohibiciones

Un plan **no puede**:

1. Redefinir la arquitectura o los límites de servicio.
2. Cambiar puertos o nombres de bases sin actualizar ESP-02 y ESP-06.
3. Proponer Eureka, mensajería, contenedores o transacciones distribuidas.
4. Modificar `database/crear_base.sql`.
5. Modificar o eliminar `backend/`.
6. Renombrar endpoints o campos del JSON.
7. Incluir fechas, plazos o estimaciones de tiempo.
8. Saltarse los criterios de aceptación de las especificaciones.
9. Introducir dependencias no justificadas por una especificación.
10. Dejar tareas sin verificación.

---

## 5. Reglas de contenido técnico

### 5.1 Contrato

- Todo plan que toque un endpoint debe citar **ESP-04**.
- Si cambia la forma de una respuesta, la tarea incluye actualizar OpenAPI y
  Postman en el mismo plan.
- Los objetos anidados (`puesto`, `socio`, `servicio`, `banco`, `usuario`,
  `cuentaCobrar`) son obligatorios y se verifican explícitamente.

### 5.2 Datos

- Todo plan que toque persistencia cita **ESP-06**.
- Las migraciones son inmutables: un cambio de esquema es una migración nueva.
- Los seeds se actualizan con una migración nueva, no editando la anterior.
- `database/crear_base.sql` no se toca.

### 5.3 Seguridad

- Todo plan que toque endpoints cita **ESP-05**.
- Las reglas de rol se aplican en el servicio, no en el gateway.
- El `JWT_SECRET` no se escribe en el repositorio.

### 5.4 Configuración

- Todo puerto, URL o credencial se declara como variable de entorno.
- Los valores por defecto se toman de **ESP-02**.
- Un plan nunca fija un puerto en el código de forma literal.

### 5.5 Riesgos

- Todo plan enumera los riesgos de **ESP-12** que aplican.
- Cada riesgo listado incluye la mitigación **concreta dentro de este plan**.
- Un riesgo crítico sin mitigación impide cerrar el plan.

---

## 6. Criterios de aceptación

### 6.1 Reglas

| Regla | Detalle |
|---|---|
| Verificables | Se pueden comprobar con un comando, una respuesta o una inspección |
| Binarios | Se cumplen o no se cumplen |
| Independientes del tiempo | Sin fechas ni plazos |
| Trazables | Cada criterio cita la especificación que lo origina |
| Completos | Cubren funcionalidad, contrato, datos y seguridad |

### 6.2 Formato

```markdown
| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | El login devuelve un JWT válido a través del gateway | `POST /api/auth/login` responde 200 con `token` | ESP-05 §2 |
| CA-02 | El token es aceptado por catálogo | `GET /api/socios` con el token responde 200 | ESP-05 §7 |
```

### 6.3 Prohibiciones

- “Funciona correctamente”.
- “Queda listo”.
- “Se prueba manualmente”.
- Cualquier criterio que dependa de una fecha.

---

## 7. Verificación y reversa

### 7.1 Verificación

Cada plan incluye:

1. **Verificación por tarea:** dentro de la propia tarea.
2. **Verificación de plan:** al final, con los comandos que prueban los
   criterios de aceptación.
3. **Verificación de contrato:** comparación del OpenAPI exportado.
4. **Verificación de regresión:** la suite de Postman del flujo completo.

### 7.2 Reversa

Todo plan debe poder deshacerse sin dejar el repositorio en un estado
inconsistente.

| Elemento | Cómo se revierte |
|---|---|
| Código nuevo | Eliminar el proyecto o revertir el commit |
| Migración aplicada | En desarrollo, `DROP DATABASE` y reaplicar; nunca editar la migración |
| Ruta del gateway | Volver a enrutar al monolito o al servicio anterior |
| Configuración | Restaurar variables de entorno |
| Contrato | El gateway reenruta; el frontend no cambia |

---

## 8. Relación entre planes

### 8.1 Dependencias

- Un plan declara qué planes deben estar cerrados antes.
- No se puede comenzar un plan cuyas precondiciones incluyan otro plan abierto.
- Un plan puede cerrar parcialmente si su alcance lo permite, siempre que sus
  criterios de aceptación se cumplan.

### 8.2 Planes previstos

| Plan | Tema | Especificaciones base | Depende de |
|---|---|---|---|
| PLAN-00 | Fundaciones, correcciones documentales y línea base del contrato | ESP-02, ESP-06, ESP-08, ESP-09, ESP-13 | — |
| PLAN-01 | API Gateway y traslado del frontend al gateway | ESP-05, ESP-07, ESP-10, ESP-11 | PLAN-00 |
| PLAN-02 | identity-service | ESP-03, ESP-04, ESP-05, ESP-08 | PLAN-00, PLAN-01 |
| PLAN-03 | catalog-service | ESP-01, ESP-03, ESP-04, ESP-05, ESP-08 | PLAN-00, PLAN-01 |
| PLAN-04 | cash-service: cuentas por cobrar, recibos y correlativos | ESP-03, ESP-04, ESP-05, ESP-08 | PLAN-00, PLAN-01, PLAN-03 |
| PLAN-05 | cash-service: tesorería, egresos y reportes | ESP-03, ESP-04, ESP-08 | PLAN-04 |
| PLAN-06 | Contrato consolidado y Swagger agregado | ESP-04, ESP-07, ESP-08 | PLAN-02, PLAN-03, PLAN-05 |
| PLAN-07 | Postman completo, regresión y demostración | ESP-00, ESP-04, ESP-09, ESP-11, ESP-13 | PLAN-06 |

Esta tabla es la referencia de secuencia. Cada plan se escribe en su propio
archivo cuando corresponda.

### 8.3 Desviaciones respecto de la secuencia original

La versión anterior de esta tabla preveía siete planes. La secuencia vigente
tiene ocho, por tres motivos que se apoyan en las propias especificaciones:

| Cambio | Motivo | Referencia |
|---|---|---|
| El frontend se integra al plan del gateway | Solo cambian dos archivos y el frontend se traslada en el Estado 1 del strangler | ESP-10 §3.3, ESP-11 §2.2 |
| Caja se parte en cobranza y tesorería | Caja concentra 25 de los 52 endpoints; el punto de corte ya está documentado | ESP-01 §7, ESP-11 §4.3 |
| OpenAPI se cierra por servicio, no al final | La definición de terminado exige que cada servicio cierre con su contrato exportado | ESP-13 §6 |

La regla que se mantiene es la misma: ningún plan decide arquitectura y todos
citan los IDs que aplican.

---

## 9. Índice de planes

| Plan | Archivo | Estado | Depende de |
|---|---|---|---|
| PLAN-00 | [00-fundaciones-y-linea-base.md](00-fundaciones-y-linea-base.md) | Escrito | — |
| PLAN-01 | [01-api-gateway-y-frontend.md](01-api-gateway-y-frontend.md) | Escrito | PLAN-00 |
| PLAN-02 | [02-identity-service.md](02-identity-service.md) | Escrito | PLAN-00, PLAN-01 |
| PLAN-03 | [03-catalog-service.md](03-catalog-service.md) | Escrito | PLAN-00, PLAN-01 |
| PLAN-04 | [04-cash-service-cobranza.md](04-cash-service-cobranza.md) | Escrito | PLAN-00, PLAN-01, PLAN-03 |
| PLAN-05 | [05-cash-service-tesoreria-reportes.md](05-cash-service-tesoreria-reportes.md) | Escrito | PLAN-04 |
| PLAN-06 | [06-contrato-y-swagger-agregado.md](06-contrato-y-swagger-agregado.md) | Escrito | PLAN-02, PLAN-03, PLAN-05 |
| PLAN-07 | [07-postman-regresion-y-demo.md](07-postman-regresion-y-demo.md) | Escrito | PLAN-06 |

> El estado solo puede ser: `No escrito`, `Escrito`, `En ejecución`, `Cerrado`
> o `Bloqueado`. Un plan `Bloqueado` indica la precondición que falta.

---

## 10. Checklist antes de dar un plan por escrito

- [ ] Tiene las doce secciones obligatorias.
- [ ] Cita todas las especificaciones que aplican.
- [ ] Declara precondiciones verificables.
- [ ] Enumera riesgos de ESP-12 con mitigación concreta.
- [ ] Las tareas son atómicas y tienen archivos explícitos.
- [ ] Cada tarea tiene verificación.
- [ ] Los criterios de aceptación son binarios.
- [ ] Hay plan de reversa.
- [ ] No contiene fechas ni plazos.
- [ ] No redefine arquitectura.
- [ ] No modifica `crear_base.sql` ni `backend/`.
- [ ] No fija puertos o secretos en el código.
- [ ] Está registrado en el índice de la sección 9.

## 11. Checklist antes de dar un plan por cerrado

- [ ] Todos los criterios de aceptación se cumplen.
- [ ] La verificación de contrato está actualizada.
- [ ] La suite Postman pasa.
- [ ] La reversa fue probada o es trivialmente aplicable.
- [ ] Los entregables existen en las rutas declaradas.
- [ ] Las especificaciones se actualizaron si hubo decisiones nuevas.
- [ ] El monolito sigue siendo ejecutable.
- [ ] El índice de la sección 9 refleja el estado `Cerrado`.
