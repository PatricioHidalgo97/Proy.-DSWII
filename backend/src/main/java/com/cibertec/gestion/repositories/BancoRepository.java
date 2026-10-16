package com.cibertec.gestion.repositories;

import com.cibertec.gestion.entities.BancoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BancoRepository extends JpaRepository<BancoEntity, Long> {
    boolean existsByNumeroCuenta(String numeroCuenta);
    boolean existsByCci(String cci);
    boolean existsByNumeroCuentaAndIdNot(String numeroCuenta, Long id);
    boolean existsByCciAndIdNot(String cci, Long id);
}
