package com.cibertec.gestion.services;

import com.cibertec.gestion.entities.EgresoEntity;
import com.cibertec.gestion.models.EgresoRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

public interface IEgresoService {
    List<EgresoEntity> getByMonth(YearMonth month);
    EgresoEntity getById(Long id);
    EgresoEntity create(EgresoRequest request, String username);
    EgresoEntity process(Long id);
    EgresoEntity annul(Long id);
    List<EgresoEntity> importCsv(MultipartFile file, String username);
}
