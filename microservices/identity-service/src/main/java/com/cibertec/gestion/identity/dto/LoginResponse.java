package com.cibertec.gestion.identity.dto;

/**
 * Forma exacta de ESP-04 §5.1. El orden de los campos es el del contrato.
 */
public record LoginResponse(
        String token,
        Long id,
        String usuario,
        String nombres,
        String apellidos,
        String rol
) {
}
