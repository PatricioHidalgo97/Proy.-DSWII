package com.cibertec.gestion.cash.clients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Vista remota mínima de un servicio (ESP-03 §4.4). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServicioRemoto(
        Long id,
        String nombre,
        String moneda,
        String cargoA,
        boolean dependeConsumo,
        BigDecimal costo,
        boolean activo
) {
}
