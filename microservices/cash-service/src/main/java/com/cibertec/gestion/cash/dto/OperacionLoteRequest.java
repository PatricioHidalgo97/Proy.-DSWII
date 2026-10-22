package com.cibertec.gestion.cash.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Operación sobre varias cuentas (pago o exoneración en lote). */
@Getter
@Setter
public class OperacionLoteRequest {

    @NotEmpty(message = "Seleccione al menos una cuenta")
    private List<Long> cuentaIds;
}
