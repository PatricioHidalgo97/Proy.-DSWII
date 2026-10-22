package com.cibertec.gestion.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de POST /api/auth/login (ESP-05 §2.1). Los nombres de campo no
 * cambian: el frontend envía usuario y password.
 */
public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        String usuario,
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
