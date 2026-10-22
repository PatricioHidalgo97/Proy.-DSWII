package com.cibertec.gestion.cash.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Canje bancario simulado (ESP-04 §4.3). */
@Getter
@Setter
public class CanjeRequest {

    @NotNull(message = "La cuenta es obligatoria")
    private Long cuentaId;

    @NotNull(message = "El banco es obligatorio")
    private Long bancoId;

    @NotNull(message = "La fecha de depósito es obligatoria")
    private LocalDate fechaDeposito;
}
