package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.LoginRequest;
import com.cibertec.gestion.models.LoginResponse;
import com.cibertec.gestion.repositories.UsuarioRepository;
import com.cibertec.gestion.security.JwtService;
import com.cibertec.gestion.services.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {
        UsuarioEntity usuario = usuarioRepository.findByUsuario(request.getUsuario())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
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
    public UsuarioEntity findByUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
