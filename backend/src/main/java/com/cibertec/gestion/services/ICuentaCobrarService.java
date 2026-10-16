package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.CuentaCobrarEntity;
import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.models.CuentaRequest;
import com.cibertec.gestion.models.CanjeRequest;
import com.cibertec.gestion.models.GenerarCuentasRequest;

import java.util.List;

public interface ICuentaCobrarService {
    List<CuentaCobrarEntity> getAll();
    List<CuentaCobrarEntity> getByPuesto(Long puestoId);
    List<CuentaCobrarEntity> getBySocio(Long socioId);
    CuentaCobrarEntity create(CuentaRequest request);
    List<CuentaCobrarEntity> generateForPuestos(GenerarCuentasRequest request);
    List<CuentaCobrarEntity> generateForSocios(GenerarCuentasRequest request);
    CuentaCobrarEntity exonerate(Long id);
    List<CuentaCobrarEntity> exonerateBatch(List<Long> ids);
    ReciboEntity pay(Long id, String username);
    List<ReciboEntity> payBatch(List<Long> ids, String username);
    ReciboEntity exchange(CanjeRequest request, String username);
}
