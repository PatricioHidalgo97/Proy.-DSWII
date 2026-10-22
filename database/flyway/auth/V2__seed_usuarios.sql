-- ============================================================================
-- identity-service / gestion_auth
-- V2: usuarios iniciales.
--   admin  / Admin123*  -> ADMIN
--   caja   / Caja123*   -> OPERADOR
--   caja2  / Demo123*   -> OPERADOR
--   caja3  / Demo123*   -> OPERADOR
-- Las contraseñas están cifradas con BCrypt (factor 10).
-- ============================================================================

INSERT INTO usuarios (id, usuario, password, nombres, apellidos, rol, activo) VALUES
  (1, 'admin', '$2b$10$PKYqXyNJGgrxLMeYddMhbe9kU9NhkVaBj8XnkenJmCmlUf0OqdGi6', 'Administrador', 'General', 'ADMIN', b'1'),
  (2, 'caja', '$2b$10$.DXaRCZ1037Ovp9OLZ.gW.8/Mdr9d9qYyvYnMaXn6pNX3cJQHMRPG', 'Operador', 'Caja', 'OPERADOR', b'1'),
  (3, 'caja2', '$2b$10$MV78Rerg1FL6Cwjv8jsB.e.xfyFimcOuORQg5eQEK.sgvc0ILlHVS', 'Lucía', 'Operadora', 'OPERADOR', b'1'),
  (4, 'caja3', '$2b$10$MV78Rerg1FL6Cwjv8jsB.e.xfyFimcOuORQg5eQEK.sgvc0ILlHVS', 'Marco', 'Operador', 'OPERADOR', b'1');
