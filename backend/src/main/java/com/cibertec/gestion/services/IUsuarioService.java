package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.models.LoginRequest;
import com.cibertec.gestion.models.LoginResponse;

public interface IUsuarioService {
    LoginResponse login(LoginRequest request);
    UsuarioEntity findByUsuario(String usuario);
}
