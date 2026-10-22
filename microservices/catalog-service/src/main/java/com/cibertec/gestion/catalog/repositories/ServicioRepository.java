package com.cibertec.gestion.catalog.repositories;

import com.cibertec.gestion.catalog.entities.ServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioRepository extends JpaRepository<ServicioEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
