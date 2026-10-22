package com.cibertec.gestion.cash.clients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vista remota mínima de un socio (ESP-03 §4.2). {@code accion} se incluye
 * porque el reporte de socios conserva las mismas columnas que el monolito
 * (ESP-04 §4.6).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SocioRemoto(
        Long id,
        String codigo,
        String nombres,
        String apellidos,
        String accion,
        Integer etapa,
        boolean activo
) {
}
