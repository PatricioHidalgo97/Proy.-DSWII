package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.PuestoEntity;
import com.cibertec.gestion.models.PuestoRequest;

import java.util.List;

public interface IPuestoService {
    List<PuestoEntity> getAll();
    PuestoEntity getById(Long id);
    PuestoEntity create(PuestoRequest request);
    PuestoEntity update(Long id, PuestoRequest request);
    void delete(Long id);
}
