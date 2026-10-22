package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.BancoRequest;
import com.cibertec.gestion.catalog.entities.BancoEntity;

import java.util.List;

public interface IBancoService {
    List<BancoEntity> getAll();
    BancoEntity getById(Long id);
    BancoEntity create(BancoRequest request);
    BancoEntity update(Long id, BancoRequest request);
    void delete(Long id);
}
