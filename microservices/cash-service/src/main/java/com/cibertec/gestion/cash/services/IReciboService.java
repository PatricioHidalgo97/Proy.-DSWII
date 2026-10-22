package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.IngresoExternoRequest;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.util.List;

/** Tres operaciones de recibos (ESP-04 §4.4). */
public interface IReciboService {

    ReciboMapper.ReciboResponse getById(Long id);

    List<ReciboMapper.ReciboResponse> getByDate(LocalDate fecha, String tipo);

    ReciboMapper.ReciboResponse createExternalIncome(IngresoExternoRequest request, UsuarioPrincipal usuario);
}
