package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.enums.TipoRecibo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReciboRepository extends JpaRepository<ReciboEntity, Long> {
    Optional<ReciboEntity> findTopByOrderByIdDesc();
    List<ReciboEntity> findByFechaBetweenOrderByFechaDesc(LocalDateTime inicio, LocalDateTime fin);
    List<ReciboEntity> findByTipoAndFechaBetweenOrderByFechaDesc(
            TipoRecibo tipo, LocalDateTime inicio, LocalDateTime fin);
}
