package com.cibertec.gestion.cash.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de caja (ESP-08 §4, §7.3). Cubre los 23 paths de
 * PLAN-04 y PLAN-05 con las cuatro tags: Cuentas, Recibos, Egresos y Reportes.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de caja",
                version = "1.0",
                description = "Cuentas por cobrar, recibos, egresos, reportes XLSX y correlativos"
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
