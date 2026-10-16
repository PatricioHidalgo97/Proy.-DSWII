package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.SocioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocioRepository extends JpaRepository<SocioEntity, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
