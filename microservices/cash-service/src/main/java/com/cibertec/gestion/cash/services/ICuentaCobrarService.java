package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.CanjeRequest;
import com.cibertec.gestion.cash.dto.CuentaRequest;
import com.cibertec.gestion.cash.dto.GenerarCuentasRequest;
import com.cibertec.gestion.cash.mappers.CuentaCobrarMapper;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;

import java.util.List;

/** Once operaciones de caja (ESP-04 §4.3). */
public interface ICuentaCobrarService {

    List<CuentaCobrarMapper.CuentaCobrarResponse> getAll();

    List<CuentaCobrarMapper.CuentaCobrarResponse> getByPuesto(Long puestoId);

    List<CuentaCobrarMapper.CuentaCobrarResponse> getBySocio(Long socioId);

    CuentaCobrarMapper.CuentaCobrarResponse create(CuentaRequest request);

    List<CuentaCobrarMapper.CuentaCobrarResponse> generateForPuestos(GenerarCuentasRequest request);

    List<CuentaCobrarMapper.CuentaCobrarResponse> generateForSocios(GenerarCuentasRequest request);

    CuentaCobrarMapper.CuentaCobrarResponse exonerate(Long id);

    List<CuentaCobrarMapper.CuentaCobrarResponse> exonerateBatch(List<Long> ids);

    ReciboMapper.ReciboResponse pay(Long id, UsuarioPrincipal usuario);

    List<ReciboMapper.ReciboResponse> payBatch(List<Long> ids, UsuarioPrincipal usuario);

    ReciboMapper.ReciboResponse exchange(CanjeRequest request, UsuarioPrincipal usuario);
}
