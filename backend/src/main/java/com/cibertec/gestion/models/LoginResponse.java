package com.cibertec.gestion.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long id;
    private String usuario;
    private String nombres;
    private String apellidos;
    private String rol;
}
