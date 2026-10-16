package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.models.LoginRequest;
import com.cibertec.gestion.models.LoginResponse;
import com.cibertec.gestion.models.UsuarioResponse;
import com.cibertec.gestion.services.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        UsuarioEntity usuario = usuarioService.findByUsuario(authentication.getName());
        return ResponseEntity.ok(new UsuarioResponse(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol().name()
        ));
    }
}
