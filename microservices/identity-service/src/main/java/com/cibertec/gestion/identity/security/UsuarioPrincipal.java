package com.cibertec.gestion.identity.security;

/**
 * Principal enriquecido con los datos del token (ESP-05 §4).
 * Evita consultar la base de datos para resolver el usuario actual.
 */
public record UsuarioPrincipal(
        Long id,
        String usuario,
        String nombres,
        String apellidos,
        String rol
) {
}
