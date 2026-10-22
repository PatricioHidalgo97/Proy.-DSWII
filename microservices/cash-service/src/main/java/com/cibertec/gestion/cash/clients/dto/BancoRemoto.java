package com.cibertec.gestion.cash.clients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista remota mínima de un banco (ESP-03 §4.5). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BancoRemoto(
        Long id,
        String nombre,
        String numeroCuenta,
        String cci,
        String moneda,
        boolean activo
) {
}
