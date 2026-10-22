-- ============================================================================
-- Sistema de Gestión Administrativa y de Caja
-- Creación de las bases de datos para la arquitectura de microservicios.
--
-- Este script NO reemplaza a database/crear_base.sql.
--   * crear_base.sql  -> base única del monolito (respaldo y ejecución legacy).
--   * este script     -> una base por microservicio + usuario compartido.
--
-- Los esquemas y los datos iniciales los administra Flyway:
--   database/flyway/auth     -> gestion_auth
--   database/flyway/catalog  -> gestion_catalog
--   database/flyway/cash     -> gestion_cash
-- ============================================================================

CREATE DATABASE IF NOT EXISTS gestion_auth
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS gestion_catalog
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS gestion_cash
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'gestion_user'@'localhost' IDENTIFIED BY 'Gestion2026*';

GRANT ALL PRIVILEGES ON gestion_auth.*    TO 'gestion_user'@'localhost';
GRANT ALL PRIVILEGES ON gestion_catalog.* TO 'gestion_user'@'localhost';
GRANT ALL PRIVILEGES ON gestion_cash.*    TO 'gestion_user'@'localhost';

FLUSH PRIVILEGES;
