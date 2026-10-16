package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.SocioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SocioRepositoryTest {

    @Autowired
    private SocioRepository socioRepository;

    @Test
    void debeInsertarSocio() {
        SocioEntity guardado = socioRepository.save(nuevoSocio("SOC-001"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(socioRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void debeListarSocios() {
        socioRepository.save(nuevoSocio("SOC-001"));
        socioRepository.save(nuevoSocio("SOC-002"));

        assertThat(socioRepository.findAll()).hasSize(2);
    }

    @Test
    void debeActualizarSocio() {
        SocioEntity socio = socioRepository.save(nuevoSocio("SOC-001"));
        socio.setNombres("Nombre actualizado");
        socio.setEtapa(2);

        SocioEntity actualizado = socioRepository.save(socio);

        assertThat(actualizado.getId()).isEqualTo(socio.getId());
        assertThat(actualizado.getNombres()).isEqualTo("Nombre actualizado");
        assertThat(actualizado.getEtapa()).isEqualTo(2);
    }

    @Test
    void debeEliminarSocio() {
        SocioEntity socio = socioRepository.save(nuevoSocio("SOC-001"));

        socioRepository.deleteById(socio.getId());

        assertThat(socioRepository.findById(socio.getId())).isEmpty();
    }

    private SocioEntity nuevoSocio(String codigo) {
        return SocioEntity.builder()
                .codigo(codigo)
                .nombres("María")
                .apellidos("Pérez")
                .accion("Titular")
                .etapa(1)
                .fechaNacimiento(LocalDate.of(1990, 5, 10))
                .activo(true)
                .build();
    }
}
