package com.cibertec.gestion.identity.security;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas equivalentes a backend/.../AuthIntegrationTest, adaptadas al usuario
 * sembrado por Flyway en gestion_auth (ESP-13 §5.1).
 */
@SpringBootTest(properties = {
        "app.jwt.secret=SistemaGestionCibertec2026ClavePruebasJWT123456789",
        "app.jwt.expiration-ms=28800000",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginCorrectoDebeDevolverToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin","password":"Admin123*"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.usuario").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void loginIncorrectoDebeDevolver401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaSinTokenDebeDevolver401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Debe iniciar sesión"));
    }

    @Test
    void tokenFirmadoConOtroSecretoDebeDevolver401() throws Exception {
        Key otraClave = Keys.hmacShaKeyFor(
                "OtroSecretoCompletamenteDistintoParaPruebas123456789".getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String tokenAjeno = Jwts.builder()
                .setSubject("admin")
                .claim("usuarioId", 1L)
                .claim("nombres", "Administrador")
                .claim("apellidos", "General")
                .claim("rol", "ADMIN")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 28_800_000L))
                .signWith(otraClave, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenAjeno))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token inválido o vencido"));
    }

    @Test
    void healthEsPublicoSinToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void restoDeActuatorNoEstaExpuesto() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin","password":"Admin123*"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/actuator/env")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.propertySources").doesNotExist());
    }
}
