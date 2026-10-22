package com.cibertec.gestion.cash.clients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vista remota mínima de un puesto (ESP-03 §6, ESP-04 §5.3). Se deserializa
 * desde {@code GET /api/puestos/{id}} de catalog-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PuestoRemoto(
        Long id,
        String numero,
        String nombreInquilino,
        SocioAnidado socio,
        GiroAnidado giro,
        boolean activo
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SocioAnidado(
            Long id,
            String codigo,
            String nombres,
            String apellidos
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GiroAnidado(
            Long id,
            String nombre
    ) {
    }
}
