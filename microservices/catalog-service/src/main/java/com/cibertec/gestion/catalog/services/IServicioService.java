package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.ServicioRequest;
import com.cibertec.gestion.catalog.entities.ServicioEntity;

import java.util.List;

public interface IServicioService {
    List<ServicioEntity> getAll();
    ServicioEntity getById(Long id);
    ServicioEntity create(ServicioRequest request);
    ServicioEntity update(Long id, ServicioRequest request);
    void delete(Long id);
}
