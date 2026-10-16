package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.CorrelativoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CorrelativoRepository extends JpaRepository<CorrelativoEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CorrelativoEntity c where c.tipo = :tipo")
    Optional<CorrelativoEntity> findByTipoForUpdate(@Param("tipo") String tipo);
}
