package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.GiroRequest;
import com.cibertec.gestion.catalog.entities.GiroEntity;
import com.cibertec.gestion.catalog.exceptions.ConflictException;
import com.cibertec.gestion.catalog.exceptions.NotFoundException;
import com.cibertec.gestion.catalog.repositories.GiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GiroService implements IGiroService {

    private final GiroRepository giroRepository;

    @Override
    public List<GiroEntity> getAll() {
        return giroRepository.findAll();
    }

    @Override
    public GiroEntity getById(Long id) {
        return giroRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Giro no encontrado"));
    }

    @Override
    public GiroEntity create(GiroRequest request) {
        if (giroRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ConflictException("Ya existe un giro con el nombre indicado");
        }
        return giroRepository.save(map(new GiroEntity(), request));
    }

    @Override
    public GiroEntity update(Long id, GiroRequest request) {
        GiroEntity giro = getById(id);
        if (giroRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre(), id)) {
            throw new ConflictException("Ya existe otro giro con el nombre indicado");
        }
        return giroRepository.save(map(giro, request));
    }

    @Override
    public void delete(Long id) {
        giroRepository.delete(getById(id));
    }

    private GiroEntity map(GiroEntity giro, GiroRequest request) {
        giro.setNombre(request.getNombre().trim());
        giro.setDescripcion(request.getDescripcion());
        giro.setActivo(request.isActivo());
        return giro;
    }
}
