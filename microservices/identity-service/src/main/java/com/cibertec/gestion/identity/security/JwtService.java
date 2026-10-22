package com.cibertec.gestion.identity.security;

import com.cibertec.gestion.identity.entities.UsuarioEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    /**
     * Longitud mínima del secreto compartido (ESP-05 §7). Un secreto más corto
     * no debe permitir el arranque del servicio.
     */
    private static final int MIN_SECRET_LENGTH = 32;

    private final Key key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.isBlank() || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET inválido: debe tener al menos " + MIN_SECRET_LENGTH + " caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UsuarioEntity usuario) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(usuario.getUsuario())
                .claim("usuarioId", usuario.getId())
                .claim("nombres", usuario.getNombres())
                .claim("apellidos", usuario.getApellidos())
                .claim("rol", usuario.getRol().name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
