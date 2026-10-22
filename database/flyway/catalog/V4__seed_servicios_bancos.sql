-- ============================================================================
-- catalog-service / gestion_catalog
-- V4: servicios y bancos de referencia.
-- ============================================================================

INSERT INTO servicios (id, nombre, recurrencia, costo, moneda, cargo_a, depende_consumo, activo) VALUES
  (1, 'Mantenimiento de puesto', 'MENSUAL', 80.00, 'PEN', 'PUESTO', b'0', b'1'),
  (2, 'Limpieza y recojo de residuos', 'MENSUAL', 35.00, 'PEN', 'PUESTO', b'0', b'1'),
  (3, 'Seguridad del mercado', 'MENSUAL', 45.00, 'PEN', 'PUESTO', b'0', b'1'),
  (4, 'Consumo de agua', 'MENSUAL', 3.50, 'PEN', 'PUESTO', b'1', b'1'),
  (5, 'Consumo de energía eléctrica', 'MENSUAL', 0.85, 'PEN', 'PUESTO', b'1', b'1'),
  (6, 'Cuota ordinaria de socio', 'MENSUAL', 50.00, 'PEN', 'SOCIO', b'0', b'1'),
  (7, 'Cuota extraordinaria de mejoras', 'UNICA', 150.00, 'PEN', 'SOCIO', b'0', b'1'),
  (8, 'Fondo de defensa gremial', 'TRIMESTRAL', 60.00, 'PEN', 'SOCIO', b'0', b'1'),
  (9, 'Cuota de aniversario', 'ANUAL', 120.00, 'PEN', 'SOCIO', b'0', b'1'),
  (10, 'Alquiler de local', 'MENSUAL', 320.00, 'PEN', 'PUESTO', b'0', b'1'),
  (11, 'Servicio de internet del puesto', 'MENSUAL', 25.00, 'PEN', 'PUESTO', b'0', b'1'),
  (12, 'Estacionamiento de socio', 'MENSUAL', 40.00, 'PEN', 'SOCIO', b'0', b'1');

INSERT INTO bancos (id, nombre, numero_cuenta, cci, moneda, activo) VALUES
  (1, 'Banco de Crédito del Perú', '193-2045678-0-11', '00219300204567801125', 'PEN', b'1'),
  (2, 'BBVA Perú', '0011-0455-0100234567', '01104550010023456718', 'PEN', b'1'),
  (3, 'Interbank', '898-3001234567', '00389800300123456789', 'PEN', b'1'),
  (4, 'Scotiabank Perú', '000-4123456', '00900000412345678901', 'PEN', b'1'),
  (5, 'Banco de la Nación', '04-123-456789', '01804000123456789012', 'PEN', b'1');
