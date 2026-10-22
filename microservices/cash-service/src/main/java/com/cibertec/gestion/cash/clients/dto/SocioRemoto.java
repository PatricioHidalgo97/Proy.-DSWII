package com.cibertec.gestion.cash.clients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista remota mínima de un socio (ESP-03 §4.2). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SocioRemoto(
        Long id,
        String codigo,
        String nombres,
        String apellidos,
        Integer etapa,
        boolean activo
) {
}
