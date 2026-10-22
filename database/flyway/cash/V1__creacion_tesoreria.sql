-- ============================================================================
-- cash-service / gestion_cash
-- V1: esquema de cuentas por cobrar, recibos, egresos y correlativos.
--
-- Este servicio es dueño de los documentos financieros. No declara claves
-- foráneas hacia gestion_catalog ni hacia gestion_auth: conserva los
-- identificadores remotos y una copia (snapshot) de los datos que necesita
-- para emitir y volver a mostrar el documento.
-- ============================================================================

CREATE TABLE correlativos (
    tipo          VARCHAR(20) NOT NULL,
    ultimo_numero BIGINT      NOT NULL,
    PRIMARY KEY (tipo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cuentas_cobrar (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    periodo               VARCHAR(20)   NOT NULL,
    monto                 DECIMAL(12,2) NOT NULL,
    estado                VARCHAR(20)   NOT NULL,
    fecha_creacion        DATETIME(6)   NOT NULL,

    puesto_id             BIGINT        NULL,
    socio_id              BIGINT        NULL,
    servicio_id           BIGINT        NOT NULL,

    lectura_inicial       DECIMAL(12,2) NULL,
    lectura_final         DECIMAL(12,2) NULL,
    costo_unitario        DECIMAL(12,4) NULL,

    -- snapshot del servicio
    servicio_nombre       VARCHAR(100)  NOT NULL,
    servicio_moneda       VARCHAR(3)    NOT NULL,
    servicio_cargo_a      VARCHAR(20)   NOT NULL,
    servicio_depende_consumo BIT(1)     NOT NULL,

    -- snapshot del puesto
    puesto_numero              VARCHAR(20)  NULL,
    puesto_nombre_inquilino    VARCHAR(150) NULL,
    puesto_giro_nombre         VARCHAR(100) NULL,
    puesto_socio_codigo        VARCHAR(20)  NULL,
    puesto_socio_nombres       VARCHAR(100) NULL,
    puesto_socio_apellidos     VARCHAR(100) NULL,

    -- snapshot del socio destinatario
    socio_codigo          VARCHAR(20)   NULL,
    socio_nombres         VARCHAR(100)  NULL,
    socio_apellidos       VARCHAR(100)  NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_puesto UNIQUE (periodo, servicio_id, puesto_id),
    CONSTRAINT uk_cuentas_socio  UNIQUE (periodo, servicio_id, socio_id),
    INDEX ix_cuentas_puesto (puesto_id),
    INDEX ix_cuentas_socio (socio_id),
    INDEX ix_cuentas_periodo (periodo),
    INDEX ix_cuentas_estado (estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recibos (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    correlativo      VARCHAR(20)   NOT NULL,
    fecha            DATETIME(6)   NOT NULL,
    monto            DECIMAL(12,2) NOT NULL,
    tipo             VARCHAR(20)   NOT NULL,

    cuenta_cobrar_id BIGINT        NULL,

    banco_id            BIGINT      NULL,
    banco_nombre        VARCHAR(100) NULL,
    banco_numero_cuenta VARCHAR(40)  NULL,
    banco_cci           VARCHAR(30)  NULL,
    banco_moneda        VARCHAR(3)   NULL,

    depositante      VARCHAR(150)  NULL,
    categoria        VARCHAR(80)   NULL,
    concepto         VARCHAR(250)  NULL,
    fecha_deposito   DATE          NULL,

    usuario_id       BIGINT        NOT NULL,
    usuario_usuario  VARCHAR(50)   NOT NULL,
    usuario_nombres  VARCHAR(100)  NOT NULL,
    usuario_apellidos VARCHAR(100) NOT NULL,
    usuario_rol      VARCHAR(20)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_recibos_correlativo UNIQUE (correlativo),
    CONSTRAINT uk_recibos_cuenta UNIQUE (cuenta_cobrar_id),
    CONSTRAINT fk_recibos_cuenta FOREIGN KEY (cuenta_cobrar_id) REFERENCES cuentas_cobrar (id),
    INDEX ix_recibos_fecha (fecha),
    INDEX ix_recibos_tipo (tipo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE egresos (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    comprobante        VARCHAR(20)   NOT NULL,
    tipo_documento     VARCHAR(20)   NOT NULL,
    numero_documento   VARCHAR(30)   NOT NULL,
    proveedor          VARCHAR(150)  NOT NULL,
    fecha              DATE          NOT NULL,
    subtotal           DECIMAL(12,2) NOT NULL,
    impuesto           DECIMAL(12,2) NOT NULL,
    total              DECIMAL(12,2) NOT NULL,
    motivo             VARCHAR(250)  NOT NULL,
    documento_asociado VARCHAR(80)   NULL,
    estado             VARCHAR(20)   NOT NULL,

    usuario_id         BIGINT        NOT NULL,
    usuario_usuario    VARCHAR(50)   NOT NULL,
    usuario_nombres    VARCHAR(100)  NOT NULL,
    usuario_apellidos  VARCHAR(100)  NOT NULL,
    usuario_rol        VARCHAR(20)   NOT NULL,

    fecha_registro     DATETIME(6)   NOT NULL,
    fecha_actualizacion DATETIME(6)  NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_egresos_comprobante UNIQUE (comprobante),
    INDEX ix_egresos_fecha (fecha),
    INDEX ix_egresos_estado (estado)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
