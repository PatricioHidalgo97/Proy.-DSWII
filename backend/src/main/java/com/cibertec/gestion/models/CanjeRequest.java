package com.cibertec.gestion.models;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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
