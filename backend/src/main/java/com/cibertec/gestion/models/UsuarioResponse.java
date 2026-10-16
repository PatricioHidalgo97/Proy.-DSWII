package com.cibertec.gestion.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String usuario;
    private String nombres;
    private String apellidos;
    private String rol;
}
