package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.CorrelativoEntity;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.repositories.CorrelativoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrelativoService {
    private final CorrelativoRepository repository;

    @Transactional
    public String next(String type, String prefix) {
        CorrelativoEntity sequence = repository.findByTipoForUpdate(type)
                .orElseThrow(() -> new BusinessException("No se configuró el correlativo " + type));
        sequence.setUltimoNumero(sequence.getUltimoNumero() + 1);
        repository.saveAndFlush(sequence);
        return "%s-%06d".formatted(prefix, sequence.getUltimoNumero());
    }
}
