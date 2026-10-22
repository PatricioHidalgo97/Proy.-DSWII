package com.cibertec.gestion.identity.services;

import com.cibertec.gestion.identity.dto.LoginRequest;
import com.cibertec.gestion.identity.dto.LoginResponse;
import com.cibertec.gestion.identity.entities.UsuarioEntity;
import com.cibertec.gestion.identity.exceptions.NotFoundException;
import com.cibertec.gestion.identity.repositories.UsuarioRepository;
import com.cibertec.gestion.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Las tres causas de fallo (usuario inexistente, inactivo, password
     * incorrecta) devuelven el mismo 401 con el mismo mensaje (ESP-05 §6).
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByUsuario(request.usuario())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        String token = jwtService.generateToken(usuario);
        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioEntity findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
