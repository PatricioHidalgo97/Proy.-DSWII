package com.cibertec.gestion.catalog.repositories;

import com.cibertec.gestion.catalog.entities.PuestoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuestoRepository extends JpaRepository<PuestoEntity, Long> {
    boolean existsByNumero(String numero);
    boolean existsByNumeroAndIdNot(String numero, Long id);
}
