package com.cibertec.gestion.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Alta de cuenta individual (ESP-04 §4.3). */
@Getter
@Setter
public class CuentaRequest {

    @NotBlank(message = "El período es obligatorio")
    @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El período debe tener formato AAAA-MM")
    private String periodo;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private Long puestoId;

    private Long socioId;

    @NotNull(message = "El servicio es obligatorio")
    private Long servicioId;

    @DecimalMin(value = "0.00", message = "La lectura inicial no puede ser negativa")
    private BigDecimal lecturaInicial;

    @DecimalMin(value = "0.00", message = "La lectura final no puede ser negativa")
    private BigDecimal lecturaFinal;

    @DecimalMin(value = "0.0001", message = "El costo unitario debe ser mayor a cero")
    private BigDecimal costoUnitario;
}
