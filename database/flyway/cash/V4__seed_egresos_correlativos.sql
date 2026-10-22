-- ============================================================================
-- cash-service / gestion_cash
-- V4: egresos y estado final de los correlativos.
--
-- Los correlativos se inicializan después de cargar recibos y egresos para
-- que el siguiente documento continúe la numeración sin colisiones.
-- ============================================================================

INSERT INTO egresos (id, comprobante, tipo_documento, numero_documento, proveedor, fecha, subtotal, impuesto, total, motivo, documento_asociado, estado, usuario_id, usuario_usuario, usuario_nombres, usuario_apellidos, usuario_rol, fecha_registro, fecha_actualizacion) VALUES
  (1, 'EGR-000001', 'FACTURA', 'F001-1001', 'Distribuidora Lima Norte S.A.C.', '2026-08-02', 100.00, 18.00, 118.00, 'Útiles de limpieza para el mercado', 'OC-001', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-02 09:07:00', '2026-08-02 11:07:00'),
  (2, 'EGR-000002', 'BOLETA', 'B001-2034', 'Ferretería El Tornillo Feliz', '2026-08-05', 230.00, 41.40, 271.40, 'Reparación de tuberías del pasillo A', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-05 09:14:00', '2026-08-05 11:14:00'),
  (3, 'EGR-000003', 'FACTURA', 'F002-4410', 'Seguridad Integral Andina E.I.R.L.', '2026-08-09', 1200.00, 216.00, 1416.00, 'Servicio de vigilancia del mes de agosto', 'OS-008', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-09 09:21:00', '2026-08-09 11:21:00'),
  (4, 'EGR-000004', 'RECIBO', 'R-0091', 'Hidrandina S.A.', '2026-08-14', 860.00, 0.00, 860.00, 'Energía eléctrica de áreas comunes', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-14 09:28:00', '2026-08-14 11:28:00'),
  (5, 'EGR-000005', 'FACTURA', 'F003-7781', 'Sedapal S.A.', '2026-08-21', 540.00, 0.00, 540.00, 'Agua de áreas comunes', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-21 09:35:00', '2026-08-21 11:35:00'),
  (6, 'EGR-000006', 'BOLETA', 'B002-1187', 'Pinturas y Acabados Sur', '2026-08-25', 310.00, 55.80, 365.80, 'Pintura de fachada principal', 'OC-002', 'ANULADO', 1, 'admin', 'Administrador', 'General', 'ADMIN', '2026-08-25 09:42:00', '2026-08-25 11:42:00'),
  (7, 'EGR-000007', 'FACTURA', 'F004-9902', 'Limpieza Total Perú S.A.C.', '2026-08-28', 980.00, 176.40, 1156.40, 'Servicio de limpieza profunda', 'OS-009', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-28 09:49:00', '2026-08-28 11:49:00'),
  (8, 'EGR-000008', 'BOLETA', 'B003-3320', 'Botica San Martín', '2026-08-30', 145.00, 26.10, 171.10, 'Botiquín de primeros auxilios', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-08-30 09:56:00', '2026-08-30 11:56:00'),
  (9, 'EGR-000009', 'FACTURA', 'F005-2210', 'Distribuidora Lima Norte S.A.C.', '2026-09-03', 180.00, 32.40, 212.40, 'Artículos de aseo', 'OC-003', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-03 09:03:00', '2026-09-03 11:03:00'),
  (10, 'EGR-000010', 'FACTURA', 'F006-5512', 'Seguridad Integral Andina E.I.R.L.', '2026-09-07', 1200.00, 216.00, 1416.00, 'Servicio de vigilancia del mes de setiembre', 'OS-010', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-07 09:10:00', '2026-09-07 11:10:00'),
  (11, 'EGR-000011', 'RECIBO', 'R-0104', 'Hidrandina S.A.', '2026-09-11', 905.00, 0.00, 905.00, 'Energía eléctrica de áreas comunes', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-11 09:17:00', '2026-09-11 11:17:00'),
  (12, 'EGR-000012', 'FACTURA', 'F007-6630', 'Constructora Andina E.I.R.L.', '2026-09-15', 2400.00, 432.00, 2832.00, 'Mantenimiento de techos del pabellón B', 'OC-004', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-15 09:24:00', '2026-09-15 11:24:00'),
  (13, 'EGR-000013', 'BOLETA', 'B004-8871', 'Ferretería El Tornillo Feliz', '2026-09-18', 420.00, 75.60, 495.60, 'Herramientas menores', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-18 09:31:00', '2026-09-18 11:31:00'),
  (14, 'EGR-000014', 'FACTURA', 'F008-1123', 'Limpieza Total Perú S.A.C.', '2026-09-22', 980.00, 176.40, 1156.40, 'Servicio de limpieza mensual', 'OS-011', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-22 09:38:00', '2026-09-22 11:38:00'),
  (15, 'EGR-000015', 'BOLETA', 'B005-4419', 'Imprenta Rápida Lima', '2026-09-26', 260.00, 46.80, 306.80, 'Paneles informativos y señalética', NULL, 'ANULADO', 1, 'admin', 'Administrador', 'General', 'ADMIN', '2026-09-26 09:45:00', '2026-09-26 11:45:00'),
  (16, 'EGR-000016', 'RECIBO', 'R-0118', 'Sedapal S.A.', '2026-09-30', 560.00, 0.00, 560.00, 'Agua de áreas comunes', NULL, 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-09-30 09:52:00', '2026-09-30 11:52:00'),
  (17, 'EGR-000017', 'FACTURA', 'F009-3340', 'Seguridad Integral Andina E.I.R.L.', '2026-10-02', 1200.00, 216.00, 1416.00, 'Servicio de vigilancia del mes de octubre', 'OS-012', 'PROCESADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-02 09:59:00', '2026-10-02 11:59:00'),
  (18, 'EGR-000018', 'FACTURA', 'F010-7788', 'Distribuidora Lima Norte S.A.C.', '2026-10-05', 210.00, 37.80, 247.80, 'Útiles de oficina de administración', 'OC-005', 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-05 09:06:00', NULL),
  (19, 'EGR-000019', 'BOLETA', 'B006-2214', 'Ferretería El Tornillo Feliz', '2026-10-08', 390.00, 70.20, 460.20, 'Reparación de puertas metálicas', NULL, 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-08 09:13:00', NULL),
  (20, 'EGR-000020', 'RECIBO', 'R-0129', 'Hidrandina S.A.', '2026-10-12', 930.00, 0.00, 930.00, 'Energía eléctrica de áreas comunes', NULL, 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-12 09:20:00', NULL),
  (21, 'EGR-000021', 'FACTURA', 'F011-9901', 'Constructora Andina E.I.R.L.', '2026-10-15', 1800.00, 324.00, 2124.00, 'Impermeabilización del pabellón C', 'OC-006', 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-15 09:27:00', NULL),
  (22, 'EGR-000022', 'BOLETA', 'B007-5561', 'Botica San Martín', '2026-10-17', 98.00, 17.64, 115.64, 'Reposición de botiquín', NULL, 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-17 09:34:00', NULL),
  (23, 'EGR-000023', 'FACTURA', 'F012-2233', 'Limpieza Total Perú S.A.C.', '2026-10-19', 980.00, 176.40, 1156.40, 'Servicio de limpieza mensual', 'OS-013', 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-19 09:41:00', NULL),
  (24, 'EGR-000024', 'BOLETA', 'B008-7789', 'Imprenta Rápida Lima', '2026-10-20', 180.00, 32.40, 212.40, 'Volantes para campaña de reciclaje', NULL, 'REGISTRADO', 2, 'caja', 'Operador', 'Caja', 'OPERADOR', '2026-10-20 09:48:00', NULL);

INSERT INTO correlativos (tipo, ultimo_numero) VALUES
  ('RECIBO', 216),
  ('EGRESO', 24);
