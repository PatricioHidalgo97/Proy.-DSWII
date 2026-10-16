package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.ServicioEntity;
import com.cibertec.gestion.models.ServicioRequest;

import java.util.List;

public interface IServicioService {
    List<ServicioEntity> getAll();
    ServicioEntity getById(Long id);
    ServicioEntity create(ServicioRequest request);
    ServicioEntity update(Long id, ServicioRequest request);
    void delete(Long id);
}
