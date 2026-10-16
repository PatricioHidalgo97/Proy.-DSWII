package com.cibertec.gestion.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bancos", "/api/socios", "/api/giros", "/api/puestos", "/api/servicios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/bancos/**", "/api/socios/**", "/api/giros/**", "/api/puestos/**", "/api/servicios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/egresos/*/anular").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"message\":\"Debe iniciar sesión\"}");
                }).accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":403,\"message\":\"No tiene permisos para esta operación\"}");
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
