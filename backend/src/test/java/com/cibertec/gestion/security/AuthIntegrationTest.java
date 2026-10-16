package com.cibertec.gestion.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginCorrectoDebeDevolverToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin_test","password":"Admin123*"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.usuario").value("admin_test"));
    }

    @Test
    void loginIncorrectoDebeDevolver401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuario":"admin_test","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaSinTokenDebeDevolver401() throws Exception {
        mockMvc.perform(get("/api/socios"))
                .andExpect(status().isUnauthorized());
    }
}
