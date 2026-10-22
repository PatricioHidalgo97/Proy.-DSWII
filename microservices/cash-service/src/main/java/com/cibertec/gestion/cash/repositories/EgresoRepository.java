package com.cibertec.gestion.cash.repositories;

import com.cibertec.gestion.cash.entities.EgresoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EgresoRepository extends JpaRepository<EgresoEntity, Long> {

    List<EgresoEntity> findByFechaBetweenOrderByFechaDesc(LocalDate inicio, LocalDate fin);
}
