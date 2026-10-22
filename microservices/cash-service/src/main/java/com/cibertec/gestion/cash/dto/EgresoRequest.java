package com.cibertec.gestion.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de egreso (ESP-04 §4.5). Las mismas validaciones y mensajes que el
 * monolito: el contrato de errores de ESP-04 §6 no cambia.
 */
@Getter
@Setter
public class EgresoRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Size(max = 20)
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 30)
    private String numeroDocumento;

    @NotBlank(message = "El proveedor es obligatorio")
    @Size(max = 150)
    private String proveedor;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal subtotal;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal impuesto;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.01", message = "El total debe ser mayor a cero")
    private BigDecimal total;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 250)
    private String motivo;

    @Size(max = 80)
    private String documentoAsociado;
}
