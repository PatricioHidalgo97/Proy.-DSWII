package com.cibertec.gestion.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SocioRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 20, message = "El código admite máximo 20 caracteres")
    private String codigo;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    @Size(max = 30)
    private String accion;

    @NotNull(message = "La etapa es obligatoria")
    @Min(value = 1, message = "La etapa mínima es 1")
    @Max(value = 3, message = "La etapa máxima es 3")
    private Integer etapa;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private boolean activo = true;
}
