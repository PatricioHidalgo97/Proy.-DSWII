package com.cibertec.gestion.identity.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI del servicio (ESP-08 §4). El esquema Bearer permite
 * usar el botón Authorize con el token de /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de identidad",
                version = "1.0",
                description = "Sesión y usuario actual"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
