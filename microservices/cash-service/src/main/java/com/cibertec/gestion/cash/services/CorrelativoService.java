package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.entities.CorrelativoEntity;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.repositories.CorrelativoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Numeración transaccional (ESP-12 R-05). El correlativo manda: nunca se lee el
 * máximo de la tabla de documentos.
 */
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
