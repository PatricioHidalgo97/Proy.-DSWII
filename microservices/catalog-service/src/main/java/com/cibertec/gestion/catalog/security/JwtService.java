package com.cibertec.gestion.catalog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Validación del JWT emitido por identity-service (ESP-05 §2.2, §7).
 * Catálogo solo verifica la firma y lee los claims: no consulta a identidad.
 */
@Service
public class JwtService {

    /**
     * Longitud mínima del secreto compartido (ESP-05 §7). Un secreto más corto
     * no debe permitir el arranque del servicio.
     */
    private static final int MIN_SECRET_LENGTH = 32;

    private final Key key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank() || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET inválido: debe tener al menos " + MIN_SECRET_LENGTH + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
