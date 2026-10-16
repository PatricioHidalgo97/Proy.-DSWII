package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.BancoEntity;
import com.cibertec.gestion.models.BancoRequest;

import java.util.List;

public interface IBancoService {
    List<BancoEntity> getAll();
    BancoEntity getById(Long id);
    BancoEntity create(BancoRequest request);
    BancoEntity update(Long id, BancoRequest request);
    void delete(Long id);
}
