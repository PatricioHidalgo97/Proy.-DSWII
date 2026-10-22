package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.SocioRequest;
import com.cibertec.gestion.catalog.entities.SocioEntity;

import java.util.List;

public interface ISocioService {
    List<SocioEntity> getAll();
    SocioEntity getById(Long id);
    SocioEntity create(SocioRequest request);
    SocioEntity update(Long id, SocioRequest request);
    void delete(Long id);
}
