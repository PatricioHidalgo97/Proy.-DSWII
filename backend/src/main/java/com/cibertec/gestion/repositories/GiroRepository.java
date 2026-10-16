package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.GiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiroRepository extends JpaRepository<GiroEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
