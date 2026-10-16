CREATE DATABASE IF NOT EXISTS sistema_gestion
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Opción recomendada: usuario exclusivo para el proyecto.
CREATE USER IF NOT EXISTS 'gestion_user'@'localhost' IDENTIFIED BY 'Gestion2026*';
GRANT ALL PRIVILEGES ON sistema_gestion.* TO 'gestion_user'@'localhost';
FLUSH PRIVILEGES;

-- Spring JPA crea y actualiza las tablas al iniciar el backend.
