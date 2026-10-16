package com.cibertec.gestion.models;

import com.cibertec.gestion.enums.Moneda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BancoRequest {
    @NotBlank(message = "El nombre del banco es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(max = 40)
    private String numeroCuenta;

    @Size(max = 30)
    private String cci;

    @NotNull(message = "La moneda es obligatoria")
    private Moneda moneda;

    private boolean activo = true;
}
