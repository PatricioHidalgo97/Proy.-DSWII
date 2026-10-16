package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.ServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
