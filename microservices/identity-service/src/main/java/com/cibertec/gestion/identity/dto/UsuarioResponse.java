package com.cibertec.gestion.identity.dto;

/**
 * Forma exacta de ESP-04 §5.2: la misma del login sin el campo token.
 */
public record UsuarioResponse(
        Long id,
        String usuario,
        String nombres,
        String apellidos,
        String rol
) {
}
