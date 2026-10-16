package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.PuestoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuestoRepository extends JpaRepository<PuestoEntity, Long> {
    boolean existsByNumero(String numero);
    boolean existsByNumeroAndIdNot(String numero, Long id);
}
