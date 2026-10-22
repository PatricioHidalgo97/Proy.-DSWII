package com.cibertec.gestion.identity.services;

import com.cibertec.gestion.identity.dto.LoginRequest;
import com.cibertec.gestion.identity.dto.LoginResponse;
import com.cibertec.gestion.identity.entities.UsuarioEntity;

public interface IUsuarioService {
    LoginResponse login(LoginRequest request);
    UsuarioEntity findByUsuario(String usuario);
}
