-- ============================================================================
-- catalog-service / gestion_catalog
-- V1: esquema de socios, giros, puestos, servicios y bancos.
--
-- Las relaciones Puesto -> Socio y Puesto -> Giro viven en esta misma base,
-- por lo que no requieren llamadas remotas ni joins distribuidos.
-- ============================================================================

CREATE TABLE socios (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    codigo           VARCHAR(20)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    accion           VARCHAR(30)  NULL,
    etapa            INT          NOT NULL,
    fecha_nacimiento DATE         NULL,
    activo           BIT(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_socios_codigo UNIQUE (codigo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE giros (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(250) NULL,
    activo      BIT(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_giros_nombre UNIQUE (nombre)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE puestos (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    numero           VARCHAR(20)  NOT NULL,
    nombre_inquilino VARCHAR(150) NULL,
    fecha_inicio     DATE         NULL,
    fecha_fin        DATE         NULL,
    socio_id         BIGINT       NULL,
    giro_id          BIGINT       NOT NULL,
    activo           BIT(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_puestos_numero UNIQUE (numero),
    CONSTRAINT fk_puestos_socio FOREIGN KEY (socio_id) REFERENCES socios (id),
    CONSTRAINT fk_puestos_giro  FOREIGN KEY (giro_id)  REFERENCES giros (id),
    INDEX ix_puestos_socio (socio_id),
    INDEX ix_puestos_giro (giro_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE servicios (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100)  NOT NULL,
    recurrencia      VARCHAR(20)   NOT NULL,
    costo            DECIMAL(12,2) NOT NULL,
    moneda           VARCHAR(3)    NOT NULL,
    cargo_a          VARCHAR(20)   NOT NULL,
    depende_consumo  BIT(1)        NOT NULL DEFAULT b'0',
    activo           BIT(1)        NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    INDEX ix_servicios_nombre (nombre)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE bancos (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100) NOT NULL,
    numero_cuenta VARCHAR(40)  NOT NULL,
    cci           VARCHAR(30)  NULL,
    moneda        VARCHAR(3)   NOT NULL,
    activo        BIT(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_bancos_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT uk_bancos_cci UNIQUE (cci)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
