package com.cibertec.gestion.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class GenerarCuentasRequest {
    @NotBlank(message = "El período es obligatorio")
    @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El período debe tener formato AAAA-MM")
    private String periodo;

    @NotNull(message = "El servicio es obligatorio")
    private Long servicioId;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private List<Long> puestoIds;
    private List<Long> socioIds;
    private Integer etapa;
    private boolean sociosUnicos;
}
