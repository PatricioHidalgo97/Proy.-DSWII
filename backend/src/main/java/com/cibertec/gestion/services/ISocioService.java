package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.SocioEntity;
import com.cibertec.gestion.models.SocioRequest;

import java.util.List;

public interface ISocioService {
    List<SocioEntity> getAll();
    SocioEntity getById(Long id);
    SocioEntity create(SocioRequest request);
    SocioEntity update(Long id, SocioRequest request);
    void delete(Long id);
}
