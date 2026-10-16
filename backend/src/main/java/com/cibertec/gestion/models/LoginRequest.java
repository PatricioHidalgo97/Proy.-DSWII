package com.cibertec.gestion.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    private String usuario;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
