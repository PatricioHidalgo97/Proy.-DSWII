package com.cibertec.gestion.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class IngresoExternoRequest {
    @NotBlank(message = "El depositante es obligatorio")
    @Size(max = 150)
    private String depositante;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 80)
    private String categoria;

    @NotBlank(message = "El concepto es obligatorio")
    @Size(max = 250)
    private String concepto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;
}
