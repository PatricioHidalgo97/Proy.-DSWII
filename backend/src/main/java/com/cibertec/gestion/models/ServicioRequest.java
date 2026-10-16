package com.cibertec.gestion.models;

import com.cibertec.gestion.enums.CargoA;
import com.cibertec.gestion.enums.Moneda;
import com.cibertec.gestion.enums.Recurrencia;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServicioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotNull(message = "La recurrencia es obligatoria")
    private Recurrencia recurrencia;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.01", message = "El costo debe ser mayor a cero")
    private BigDecimal costo;

    @NotNull(message = "La moneda es obligatoria")
    private Moneda moneda;

    @NotNull(message = "El destino del cargo es obligatorio")
    private CargoA cargoA;

    private boolean dependeConsumo;
    private boolean activo = true;
}
