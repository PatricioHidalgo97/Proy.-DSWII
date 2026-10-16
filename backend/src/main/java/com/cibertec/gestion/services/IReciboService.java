package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.enums.TipoRecibo;
import com.cibertec.gestion.models.IngresoExternoRequest;

import java.time.LocalDate;
import java.util.List;

public interface IReciboService {
    ReciboEntity getById(Long id);
    List<ReciboEntity> getByDate(LocalDate date, TipoRecibo tipo);
    ReciboEntity createExternalIncome(IngresoExternoRequest request, String username);
}
