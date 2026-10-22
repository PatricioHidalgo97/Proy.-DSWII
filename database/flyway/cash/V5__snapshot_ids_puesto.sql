-- ============================================================================
-- cash-service / gestion_cash
-- V5: identificadores del socio y del giro del puesto en el snapshot de la
--     cuenta por cobrar, con relleno de los documentos ya emitidos.
--
-- Propósito (PLAN-09, ESP-04 §5.4): la cuenta por cobrar publica
-- "puesto.socio.id" y "puesto.giro.id" como parte obligatoria de su forma
-- (docs/contract/baseline-contrato.md §1.4). V1 las guardó como snapshot de
-- texto (puesto_socio_codigo, puesto_giro_nombre) pero no persistió los ids,
-- así que la respuesta los serializaba como null. Esta migración añade las dos
-- columnas y reconstruye sus valores en las filas ya emitidas.
--
-- Por qué el relleno lee gestion_catalog: es una lectura de UNA SOLA VEZ,
-- dentro de una migración de datos históricos, para reconstruir el snapshot de
-- documentos ya emitidos. No se repite nunca en el camino de lectura ni de
-- listado: cash-service no consulta catálogo al leer (ESP-03 §5, §8), y el
-- histórico se sigue mostrando con catalog-service detenido (ESP-03 §10).
--
-- Reglas que respeta:
--   * Las dos columnas son NULL: una cuenta por socio no tiene puesto, y un
--     puesto cuyo giro o socio ya no existe en el catálogo conserva su snapshot
--     de texto sin inventar un id (ESP-03 §10, ESP-12 R-10).
--   * El relleno usa INNER JOIN por igualdad exacta contra columnas únicas
--     (giros.nombre = uk_giros_nombre, socios.codigo = uk_socios_codigo). No
--     hay LEFT JOIN, COALESCE, LIKE ni valores por defecto.
--   * No se crean claves foráneas hacia gestion_catalog ni hacia otro esquema
--     (ESP-03 §3): los ids remotos son números sin FK.
--   * No se crean índices: las columnas no se usan en WHERE.
--   * V1 a V4 no se editan (ESP-06 §4).
-- ============================================================================

ALTER TABLE cuentas_cobrar
    ADD COLUMN puesto_giro_id  BIGINT NULL AFTER puesto_giro_nombre,
    ADD COLUMN puesto_socio_id BIGINT NULL AFTER puesto_giro_id;

-- Giro del puesto: nombre del snapshot -> id del catálogo.
UPDATE cuentas_cobrar c
    INNER JOIN gestion_catalog.giros g ON g.nombre = c.puesto_giro_nombre
SET c.puesto_giro_id = g.id
WHERE c.puesto_id IS NOT NULL;

-- Socio del puesto: código del snapshot -> id del catálogo.
UPDATE cuentas_cobrar c
    INNER JOIN gestion_catalog.socios s ON s.codigo = c.puesto_socio_codigo
SET c.puesto_socio_id = s.id
WHERE c.puesto_id IS NOT NULL;
