# PLAN-07 — Postman completo, regresión y demostración

## 1. Objetivo

Cerrar la migración con una colección Postman que recorra el flujo completo por
el gateway, con asserts de contrato y casos negativos, ejecutable con Newman; y
con una demostración reproducible de principio a fin sin pasos manuales de
datos.

## 2. Especificaciones aplicables

| ID | Sección | Qué se aplica |
|---|---|---|
| ESP-09 | §1–§9 | Colección, entorno, estructura, asserts, Newman |
| ESP-11 | §3 (F4), §7 (Fase 4), §8 | Cierre de la migración |
| ESP-13 | §5, §6, §6.1 | Pruebas, definición de terminado global |
| ESP-04 | §5, §6, §8 | Contrato verificado por asserts |
| ESP-12 | R-01, R-10, R-12 | Riesgos aplicables |
| ESP-00 | §5 | Criterios de éxito del proyecto |

## 3. Alcance

### Dentro

- Las ocho carpetas de ESP-09 §3 completas.
- Asserts de código y de al menos un campo por petición.
- Encadenamiento de ids mediante variables de colección.
- Casos negativos de seguridad: 401, 403, 404, 400 con `errors`, 409.
- Asserts de contrato sobre campos anidados.
- Validación de los cinco XLSX.
- Ejecución con Newman y reporte HTML.
- Guion de demostración con el flujo de ESP-00 §5.5.
- Actualización del índice de planes y del README raíz.

### Fuera

- Endpoints nuevos o cambios de contrato.
- Pruebas de carga o de concurrencia masiva.
- Video de demostración.
- Sustituir las pruebas unitarias por Postman.

## 4. Precondiciones

| Condición | Cómo se comprueba |
|---|---|
| PLAN-00 a PLAN-06 cerrados | Índice de `docs/plans/README.md` |
| Los cuatro servicios y el frontend arrancan | Health de 9101–9104 y `http://localhost:4400` |
| Seeds aplicados desde cero | Conteos de PLAN-00 §9.1 |
| Contrato verificado | `docs/contract/verificacion-contrato.md` |
| `npx` disponible | `npx --version` |

## 5. Riesgos y mitigaciones

| Riesgo | Impacto | Mitigación dentro de este plan |
|---|---|---|
| R-01 Contrato roto | Alto | T-02 agrega los asserts de anidamiento de ESP-09 §4.8; T-04 confirma que ninguna respuesta queda sin assert |
| R-10 Datos de demostración incoherentes | Medio | T-06 reinicia la base desde cero y recorre el flujo antes de ensayar la demo |
| R-12 Contratos divergentes | Medio | T-05 compara la colección contra el OpenAPI consolidado del PLAN-06 |

## 6. Tareas

### T-01 — Completar la carpeta de seguridad

- **Objetivo:** cubrir los seis casos de ESP-09 §4.7.
- **Especificación:** ESP-09 §3, §4.7.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** PLAN-06.
- **Pasos:**
  1. `GET /api/socios` sin token: 401.
  2. `GET /api/socios` con token inválido: 401.
  3. `POST /api/socios` con token de operador: 403.
  4. `DELETE /api/socios/{id}` con token de operador: 403.
  5. `GET /api/socios/999999`: 404.
  6. `POST /api/socios` con código vacío: 400 con `errors`.
  7. `POST /api/socios` duplicado: 409.
- **Verificación:** los siete ítems pasan y cada uno valida el código y el campo `message`.
- **Terminado cuando:** la carpeta `07 · Seguridad` está completa.

### T-02 — Completar la carpeta de contrato

- **Objetivo:** verificar el anidamiento en las respuestas que usa Angular.
- **Especificación:** ESP-09 §3, §4.8; ESP-04 §5.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-01.
- **Pasos:**
  1. En cuentas, verificar `puesto`, `socio` y `servicio` presentes, y `lecturaInicial`, `lecturaFinal` y `costoUnitario` presentes aunque sean nulos.
  2. En puestos, verificar `socio` y `giro` anidados.
  3. En recibos, verificar `cuentaCobrar`, `banco` y `usuario`.
  4. En egresos, verificar `usuario` con sus cinco campos.
  5. Verificar que ningún campo está omitido cuando su valor es `null`.
- **Verificación:** la carpeta `08 · Contrato` pasa completa.
- **Terminado cuando:** los cinco grupos de asserts están en verde.

### T-03 — Encadenar y aislar la colección

- **Objetivo:** que la colección se pueda ejecutar varias veces sin limpiar datos.
- **Especificación:** ESP-09 §4, §7, §7.1, §8.
- **Archivos:** `docs/postman/Sistema_Gestion_Microservicios.postman_collection.json`
- **Depende de:** T-01.
- **Pasos:**
  1. Verificar que cada alta guarda su id en una variable de colección.
  2. Usar los sufijos `PM-` de ESP-09 §7.1 en todos los datos creados.
  3. Agregar un pre-request que genere un sufijo único por ejecución.
  4. Ejecutar la colección dos veces seguidas y confirmar que ambas pasan.
- **Verificación:** dos ejecuciones consecutivas en verde sin tocar la base.
- **Terminado cuando:** la colección es repetible.

### T-04 — Verificar la cobertura de endpoints

- **Objetivo:** demostrar que ningún endpoint público queda sin petición.
- **Especificación:** ESP-09 §8.
- **Archivos:** `docs/contract/cobertura-postman.md`
- **Depende de:** T-02.
- **Pasos:**
  1. Listar los 52 endpoints de la línea base.
  2. Marcar cuál petición de la colección lo cubre.
  3. Marcar si tiene assert de código y de campo.
  4. Registrar los que falten y agregarlos.
- **Verificación:** las 52 filas tienen petición asignada.
- **Terminado cuando:** la cobertura es del 100 %.

### T-05 — Ejecutar Newman y guardar el reporte

- **Objetivo:** dejar evidencia ejecutable del cierre.
- **Especificación:** ESP-09 §6, §6.1.
- **Archivos:** `docs/postman/reporte.html`
- **Depende de:** T-01 a T-04.
- **Pasos:**
  1. Ejecutar el comando de ESP-09 §6 con los dos reporters.
  2. Verificar cero fallos.
  3. Guardar el reporte en `docs/postman/reporte.html`.
- **Verificación:** el reporte muestra todas las peticiones en verde.
- **Terminado cuando:** el reporte existe y no tiene fallos.

### T-06 — Ensayar la demostración desde cero

- **Objetivo:** que la presentación no dependa de datos creados a mano.
- **Especificación:** ESP-00 §5.5; ESP-06 §6.3, §7; ESP-11 §7 (Fase 4).
- **Archivos:** `docs/demo-guion.md`
- **Depende de:** T-05.
- **Pasos:**
  1. Ejecutar el reinicio limpio de ESP-06 §6.3.
  2. Arrancar los cuatro servicios y el frontend.
  3. Recorrer el flujo: login, catálogos, deuda, pago, recibo, tesorería y reporte.
  4. Registrar cada pantalla con los datos sembrados que la alimentan.
  5. Anotar cualquier paso que requiera crear datos a mano; si existe, corregir los seeds con una migración nueva y repetir.
- **Verificación:** el flujo completo se ejecuta sin crear datos manualmente.
- **Terminado cuando:** el guion se ejecuta de principio a fin sobre una base recién creada.

### T-07 — Actualizar la documentación de cierre

- **Objetivo:** dejar el repositorio coherente con el estado final.
- **Especificación:** ESP-13 §6; ESP-11 §7 (Fase 4).
- **Archivos:**
  - `docs/plans/README.md` (índice con los ocho planes y su estado)
  - `README.md` (raíz: arranque de microservicios)
  - `microservices/README.md`
- **Depende de:** T-06.
- **Pasos:**
  1. Registrar los ocho planes en el índice con estado `Cerrado`.
  2. Actualizar el README raíz con el nuevo arranque en cinco terminales.
  3. Conservar en el README la ejecución del monolito como respaldo.
  4. Enlazar `docs/contract/` y `docs/postman/` desde el README.
- **Verificación:** un lector nuevo puede levantar el sistema siguiendo el README.
- **Terminado cuando:** el README describe el estado final y el índice cuadra.

## 7. Orden de ejecución

| Nivel | Tareas | Nota |
|---|---|---|
| 1 | T-01, T-03 | Seguridad y aislamiento de la colección |
| 2 | T-02 | Asserts de contrato |
| 3 | T-04 | Cobertura de los 52 endpoints |
| 4 | T-05 | Ejecución de Newman y reporte |
| 5 | T-06 | Ensayo sobre base recién creada |
| 6 | T-07 | Documentación de cierre, última porque refleja todo lo anterior |

T-06 es la única tarea que reinicia la base; debe ejecutarse después de guardar
el reporte de Newman.

## 8. Criterios de aceptación

| ID | Criterio | Verificación | Especificación |
|---|---|---|---|
| CA-01 | La colección tiene las ocho carpetas | Inspección del JSON | ESP-09 §3 |
| CA-02 | Los 52 endpoints tienen al menos una petición | `docs/contract/cobertura-postman.md` al 100 % | ESP-09 §8 |
| CA-03 | Cada petición valida código y un campo | Revisión de los scripts de test | ESP-09 §8 |
| CA-04 | Los siete casos de seguridad pasan | Carpeta `07 · Seguridad` en verde | ESP-09 §4.7 |
| CA-05 | Los cinco grupos de contrato pasan | Carpeta `08 · Contrato` en verde | ESP-09 §4.8 |
| CA-06 | La colección se ejecuta dos veces seguidas | Dos corridas en verde | ESP-09 §8 |
| CA-07 | Newman termina sin fallos | Salida con `0 failures` | ESP-09 §6 |
| CA-08 | El reporte HTML existe | `docs/postman/reporte.html` | ESP-09 §6 |
| CA-09 | La demo corre sobre base recién creada | Flujo completo sin datos manuales | ESP-00 §5.5 |
| CA-10 | El frontend funciona solo por el gateway | Ninguna llamada a 9090 | ESP-00 §5.1 |
| CA-11 | El monolito sigue siendo ejecutable | Arranca en 9090 con su perfil | ESP-00 §5.7 |
| CA-12 | El índice de planes está completo | Ocho filas en estado `Cerrado` | ESP-13 §6 |

## 9. Verificación

```powershell
# Arranque completo
cd backend                        ; mvn spring-boot:run     # 9090 (respaldo)
cd ../microservices/identity-service ; mvn spring-boot:run  # 9102
cd ../catalog-service             ; mvn spring-boot:run      # 9103
cd ../cash-service                ; mvn spring-boot:run      # 9104
cd ../api-gateway                 ; mvn spring-boot:run      # 9101
cd ../../frontend                 ; npm start -- --port 4400

# CA-02 y CA-04 a CA-06
npx newman run docs/postman/Sistema_Gestion_Microservicios.postman_collection.json `
  -e docs/postman/Local_Microservicios.postman_environment.json `
  --reporters cli,html `
  --reporter-html-export docs/postman/reporte.html

# CA-10: con el monolito detenido, recorrer las once pantallas
Start-Process "http://localhost:4400"

# CA-11
cd backend ; mvn spring-boot:run

# CA-12
Select-String -Path docs/plans/README.md -Pattern "Cerrado"
```

## 10. Reversa

| Falla en | Acción |
|---|---|
| T-01 a T-04 | La colección es un artefacto aislado: revertir el archivo y no afecta al sistema |
| T-06 | Volver a los seeds actuales; los datos no se editan desde la demo |
| Cualquier punto | El monolito sigue operativo, por lo que la demostración puede hacerse sobre él si fuera necesario |

## 11. Entregables

```text
docs/postman/Sistema_Gestion_Microservicios.postman_collection.json  (8 carpetas, 52 endpoints)
docs/postman/Local_Microservicios.postman_environment.json
docs/postman/reporte.html
docs/contract/cobertura-postman.md
docs/demo-guion.md
README.md                      (actualizado)
microservices/README.md        (actualizado)
docs/plans/README.md           (índice con ocho planes cerrados)
```

## 12. Fuera de alcance

- Endpoints nuevos.
- Pruebas de carga.
- Video de demostración.
- Eliminar el monolito o `crear_base.sql`.
- Cambiar los seeds sin una migración nueva.
