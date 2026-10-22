package com.cibertec.gestion.cash.security;

/**
 * Principal con los datos del token (ESP-05 §4). Caja lo usa para escribir el
 * snapshot del usuario en recibos sin consultar identidad.
 */
public record UsuarioPrincipal(
        Long id,
        String usuario,
        String nombres,
        String apellidos,
        String rol
) {
}
