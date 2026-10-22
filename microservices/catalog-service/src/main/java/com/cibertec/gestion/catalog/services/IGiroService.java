package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.GiroRequest;
import com.cibertec.gestion.catalog.entities.GiroEntity;

import java.util.List;

public interface IGiroService {
    List<GiroEntity> getAll();
    GiroEntity getById(Long id);
    GiroEntity create(GiroRequest request);
    GiroEntity update(Long id, GiroRequest request);
    void delete(Long id);
}
