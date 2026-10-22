-- ============================================================================
-- catalog-service / gestion_catalog
-- V3: puestos. Varios puestos no tienen socio asignado (relación opcional)
-- y todos tienen giro obligatorio.
-- ============================================================================

INSERT INTO puestos (id, numero, nombre_inquilino, fecha_inicio, fecha_fin, socio_id, giro_id, activo) VALUES
  (1, 'P-101', 'Rosa Quispe Mamani', '2024-01-01', NULL, 1, 1, b'1'),
  (2, 'P-102', 'Julio César Ramírez Torres', '2024-01-01', NULL, 2, 3, b'1'),
  (3, 'P-103', 'María Elena Flores Rojas', '2024-01-01', NULL, 3, 2, b'1'),
  (4, 'P-104', 'Pedro Pablo Huamán Ccahuana', '2024-01-01', NULL, 4, 4, b'1'),
  (5, 'P-105', 'Puesto vacante', '2024-01-01', NULL, NULL, 7, b'1'),
  (6, 'P-106', 'Ana Lucía Mendoza Vargas', '2024-03-01', NULL, 5, 5, b'1'),
  (7, 'P-107', 'Jorge Luis Salazar Paredes', '2024-03-01', NULL, 6, 1, b'1'),
  (8, 'P-108', 'Carmen Rosa Delgado Núñez', '2024-03-01', NULL, 7, 8, b'1'),
  (9, 'P-109', 'Luis Alberto Chávez Ríos', '2024-05-01', NULL, 8, 6, b'1'),
  (10, 'P-110', 'Gladys Yupanqui Ccorimanya', '2024-05-01', NULL, 9, 3, b'1'),
  (11, 'P-111', 'Miguel Ángel Ttito Ataucusi', '2024-05-01', NULL, 10, 4, b'1'),
  (12, 'P-112', 'Comerciante independiente', '2024-07-01', NULL, NULL, 2, b'1'),
  (13, 'P-113', 'Sonia Beatriz Ríos Alvarado', '2024-07-01', NULL, 11, 5, b'1'),
  (14, 'P-114', 'Raúl Ernesto Cárdenas Pinto', '2024-07-01', NULL, 12, 1, b'1'),
  (15, 'P-115', 'Juana Mercedes Huaraca Layme', '2025-01-01', NULL, 13, 7, b'1'),
  (16, 'P-116', 'César Augusto Villanueva Soto', '2025-01-01', NULL, 14, 2, b'1'),
  (17, 'P-117', 'Patricia Isabel Rojas Campos', '2025-01-01', NULL, 15, 4, b'1'),
  (18, 'P-118', 'Wilfredo Choque Apaza', '2025-03-01', NULL, 16, 8, b'1'),
  (19, 'P-119', 'Elizabeth Vargas Cáceres', '2025-03-01', NULL, 17, 5, b'1'),
  (20, 'P-120', 'Comerciante independiente', '2025-03-01', NULL, NULL, 6, b'1'),
  (21, 'P-121', 'Hugo Percy Medina Salas', '2025-07-01', NULL, 18, 1, b'1'),
  (22, 'P-122', 'Nelly Roxana Puma Quispe', '2025-07-01', NULL, 19, 3, b'1'),
  (23, 'P-123', 'Marco Antonio Zeballos Ferro', '2025-07-01', NULL, 20, 4, b'1'),
  (24, 'P-124', 'Comerciante independiente', '2026-01-01', NULL, NULL, 7, b'1'),
  (25, 'P-125', 'Edith Soledad Arenas Portugal', '2024-01-01', '2026-06-30', 23, 6, b'0'),
  (26, 'P-126', 'Freddy Rolando Nina Cusi', '2024-01-01', '2026-06-30', 24, 8, b'0');
