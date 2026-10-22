-- ============================================================================
-- identity-service / gestion_auth
-- V1: esquema de usuarios y roles.
-- ============================================================================

CREATE TABLE usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario    VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    nombres    VARCHAR(100) NOT NULL,
    apellidos  VARCHAR(100) NOT NULL,
    rol        VARCHAR(20)  NOT NULL,
    activo     BIT(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_usuario UNIQUE (usuario)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
