package com.cibertec.gestion.identity.controllers;

import com.cibertec.gestion.identity.dto.LoginRequest;
import com.cibertec.gestion.identity.dto.LoginResponse;
import com.cibertec.gestion.identity.dto.UsuarioResponse;
import com.cibertec.gestion.identity.security.UsuarioPrincipal;
import com.cibertec.gestion.identity.services.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Sesión y usuario actual (ESP-04 §4.1)")
public class AuthController {

    private final IUsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(
            summary = "Inicia sesión",
            description = "Autentica al usuario y devuelve un JWT firmado con HS256 (ESP-05 §2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales válidas",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiJ9...",
                                      "id": 1,
                                      "usuario": "admin",
                                      "nombres": "Administrador",
                                      "apellidos": "General",
                                      "rol": "ADMIN"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 401,
                                      "message": "Usuario o contraseña incorrectos"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Devuelve el usuario del token",
            description = "Lee los datos del principal del token, sin consultar la base de datos (ESP-05 §4).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "usuario": "admin",
                                      "nombres": "Administrador",
                                      "apellidos": "General",
                                      "rol": "ADMIN"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 401,
                                      "message": "Token inválido o vencido"
                                    }
                                    """)))
    })
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(new UsuarioResponse(
                principal.id(),
                principal.usuario(),
                principal.nombres(),
                principal.apellidos(),
                principal.rol()
        ));
    }
}
