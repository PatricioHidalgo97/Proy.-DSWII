package com.cibertec.gestion.catalog.repositories;

import com.cibertec.gestion.catalog.entities.GiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiroRepository extends JpaRepository<GiroEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
