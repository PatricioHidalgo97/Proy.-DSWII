package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.PuestoRequest;
import com.cibertec.gestion.catalog.dto.PuestoResponse;

import java.util.List;

public interface IPuestoService {
    List<PuestoResponse> getAll();
    PuestoResponse getById(Long id);
    PuestoResponse create(PuestoRequest request);
    PuestoResponse update(Long id, PuestoRequest request);
    void delete(Long id);
}
