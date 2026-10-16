package com.cibertec.gestion.config;

import com.cibertec.gestion.entities.CorrelativoEntity;
import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.enums.Rol;
import com.cibertec.gestion.repositories.CorrelativoRepository;
import com.cibertec.gestion.repositories.EgresoRepository;
import com.cibertec.gestion.repositories.ReciboRepository;
import com.cibertec.gestion.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CorrelativoRepository correlativoRepository;
    private final ReciboRepository reciboRepository;
    private final EgresoRepository egresoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.usuario}")
    private String username;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.nombres}")
    private String names;

    @Value("${app.admin.apellidos}")
    private String lastNames;

    @Value("${app.operator.usuario:caja}")
    private String operatorUsername;

    @Value("${app.operator.password:Caja123*}")
    private String operatorPassword;

    @Value("${app.operator.nombres:Operador}")
    private String operatorNames;

    @Value("${app.operator.apellidos:Caja}")
    private String operatorLastNames;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsuario(username).isEmpty()) {
            usuarioRepository.save(UsuarioEntity.builder()
                    .usuario(username)
                    .password(passwordEncoder.encode(password))
                    .nombres(names)
                    .apellidos(lastNames)
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build());
        }
        if (usuarioRepository.findByUsuario(operatorUsername).isEmpty()) {
            usuarioRepository.save(UsuarioEntity.builder()
                    .usuario(operatorUsername)
                    .password(passwordEncoder.encode(operatorPassword))
                    .nombres(operatorNames)
                    .apellidos(operatorLastNames)
                    .rol(Rol.OPERADOR)
                    .activo(true)
                    .build());
        }
        correlativoRepository.findById("RECIBO").orElseGet(() -> correlativoRepository.save(
                CorrelativoEntity.builder().tipo("RECIBO").ultimoNumero(reciboRepository.count()).build()));
        correlativoRepository.findById("EGRESO").orElseGet(() -> correlativoRepository.save(
                CorrelativoEntity.builder().tipo("EGRESO").ultimoNumero(egresoRepository.count()).build()));
    }
}
