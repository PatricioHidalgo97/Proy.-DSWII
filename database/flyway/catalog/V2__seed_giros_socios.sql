-- ============================================================================
-- catalog-service / gestion_catalog
-- V2: giros y socios base.
-- ============================================================================

INSERT INTO giros (id, nombre, descripcion, activo) VALUES
  (1, 'Abarrotes y víveres', 'Venta de productos de primera necesidad', b'1'),
  (2, 'Carnes, aves y pescados', 'Comercialización de carnes y pescados frescos', b'1'),
  (3, 'Frutas y verduras', 'Venta de productos agrícolas frescos', b'1'),
  (4, 'Comida y bebidas preparadas', 'Puestos de comida preparada y refrescos', b'1'),
  (5, 'Ropa, calzado y textiles', 'Prendas de vestir, calzado y telas', b'1'),
  (6, 'Ferretería y materiales', 'Herramientas, pinturas y materiales de construcción', b'1'),
  (7, 'Servicios generales', 'Internet, fotocopias, telefonía y afines', b'1'),
  (8, 'Flores, plantas y artesanía', 'Flores, plantas ornamentales y artesanía', b'1');

INSERT INTO socios (id, codigo, nombres, apellidos, accion, etapa, fecha_nacimiento, activo) VALUES
  (1, 'SOC-001', 'Rosa', 'Quispe Mamani', 'A-001', 1, '1978-03-12', b'1'),
  (2, 'SOC-002', 'Julio César', 'Ramírez Torres', 'A-002', 1, '1981-07-24', b'1'),
  (3, 'SOC-003', 'María Elena', 'Flores Rojas', 'A-003', 2, '1985-11-02', b'1'),
  (4, 'SOC-004', 'Pedro Pablo', 'Huamán Ccahuana', 'A-004', 2, '1975-05-18', b'1'),
  (5, 'SOC-005', 'Ana Lucía', 'Mendoza Vargas', 'A-005', 1, '1990-01-30', b'1'),
  (6, 'SOC-006', 'Jorge Luis', 'Salazar Paredes', 'A-006', 3, '1979-09-09', b'1'),
  (7, 'SOC-007', 'Carmen Rosa', 'Delgado Núñez', 'A-007', 1, '1983-02-14', b'1'),
  (8, 'SOC-008', 'Luis Alberto', 'Chávez Ríos', 'A-008', 2, '1988-06-21', b'1'),
  (9, 'SOC-009', 'Gladys', 'Yupanqui Ccorimanya', 'A-009', 3, '1972-12-05', b'1'),
  (10, 'SOC-010', 'Miguel Ángel', 'Ttito Ataucusi', 'A-010', 1, '1992-04-17', b'1'),
  (11, 'SOC-011', 'Sonia Beatriz', 'Ríos Alvarado', 'A-011', 2, '1986-08-08', b'1'),
  (12, 'SOC-012', 'Raúl Ernesto', 'Cárdenas Pinto', 'A-012', 4, '1969-03-27', b'1'),
  (13, 'SOC-013', 'Juana Mercedes', 'Huaraca Layme', 'A-013', 1, '1980-10-11', b'1'),
  (14, 'SOC-014', 'César Augusto', 'Villanueva Soto', 'A-014', 3, '1977-01-19', b'1'),
  (15, 'SOC-015', 'Patricia Isabel', 'Rojas Campos', 'A-015', 2, '1989-05-06', b'1'),
  (16, 'SOC-016', 'Wilfredo', 'Choque Apaza', 'A-016', 1, '1974-07-03', b'1'),
  (17, 'SOC-017', 'Elizabeth', 'Vargas Cáceres', 'A-017', 3, '1984-11-23', b'1'),
  (18, 'SOC-018', 'Hugo Percy', 'Medina Salas', 'A-018', 2, '1976-09-15', b'1'),
  (19, 'SOC-019', 'Nelly Roxana', 'Puma Quispe', 'A-019', 1, '1991-02-28', b'1'),
  (20, 'SOC-020', 'Marco Antonio', 'Zeballos Ferro', 'A-020', 4, '1971-06-04', b'1'),
  (21, 'SOC-021', 'Olga Marina', 'Ccama Ticona', 'A-021', 2, '1982-04-26', b'0'),
  (22, 'SOC-022', 'Dante Alberto', 'Pacheco Llerena', 'A-022', 1, '1987-12-13', b'0'),
  (23, 'SOC-023', 'Edith Soledad', 'Arenas Portugal', 'A-023', 3, '1985-03-08', b'1'),
  (24, 'SOC-024', 'Freddy Rolando', 'Nina Cusi', 'A-024', 1, '1993-08-20', b'1');
