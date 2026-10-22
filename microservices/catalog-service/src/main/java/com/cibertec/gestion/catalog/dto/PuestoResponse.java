package com.cibertec.gestion.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Forma de respuesta de puesto (ESP-04 §5.3). Conserva los objetos anidados
 * `socio` y `giro` con los mismos nombres de campo del monolito. Cuando el
 * puesto no tiene socio, el campo se serializa como `null` explícito: no se
 * omite (ESP-04 §7.5).
 */
@Schema(description = "Puesto con socio y giro anidados (ESP-04 §5.3)")
public record PuestoResponse(
        Long id,
        String numero,
        String nombreInquilino,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        SocioAnidado socio,
        GiroAnidado giro,
        boolean activo
) {

    @Schema(description = "Socio asignado al puesto; null si el puesto no tiene socio")
    public record SocioAnidado(
            Long id,
            String codigo,
            String nombres,
            String apellidos,
            String accion,
            Integer etapa,
            LocalDate fechaNacimiento,
            boolean activo
    ) {
    }

    public record GiroAnidado(
            Long id,
            String nombre,
            String descripcion,
            boolean activo
    ) {
    }
}
