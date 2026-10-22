package com.cibertec.gestion.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PuestoRequest {

    @NotBlank(message = "El número del puesto es obligatorio")
    @Size(max = 20)
    private String numero;

    @Size(max = 150)
    private String nombreInquilino;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private Long socioId;

    @NotNull(message = "El giro es obligatorio")
    private Long giroId;

    private boolean activo = true;
}
