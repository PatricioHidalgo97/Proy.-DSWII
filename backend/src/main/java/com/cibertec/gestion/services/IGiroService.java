package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.GiroEntity;
import com.cibertec.gestion.models.GiroRequest;

import java.util.List;

public interface IGiroService {
    List<GiroEntity> getAll();
    GiroEntity getById(Long id);
    GiroEntity create(GiroRequest request);
    GiroEntity update(Long id, GiroRequest request);
    void delete(Long id);
}
