package com.cibertec.gestion.cash.repositories;

import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaCobrarRepository extends JpaRepository<CuentaCobrarEntity, Long> {

    List<CuentaCobrarEntity> findByPuestoIdOrderByFechaCreacionDesc(Long puestoId);

    List<CuentaCobrarEntity> findBySocioIdOrderByFechaCreacionDesc(Long socioId);

    boolean existsByPeriodoAndServicioIdAndPuestoId(String periodo, Long servicioId, Long puestoId);

    boolean existsByPeriodoAndServicioIdAndSocioId(String periodo, Long servicioId, Long socioId);

    /**
     * Bloqueo pesimista de la cuenta antes de pagar o exonerar (ESP-12 R-05).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaCobrarEntity c where c.id = :id")
    Optional<CuentaCobrarEntity> findByIdForUpdate(@Param("id") Long id);
}
