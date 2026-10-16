package com.cibertec.gestion.models;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OperacionLoteRequest {
    @NotEmpty(message = "Seleccione al menos una cuenta")
    private List<Long> cuentaIds;
}
