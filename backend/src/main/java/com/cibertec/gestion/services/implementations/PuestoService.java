package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.GiroEntity;
import com.cibertec.gestion.entities.PuestoEntity;
import com.cibertec.gestion.entities.SocioEntity;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.exceptions.ConflictException;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.PuestoRequest;
import com.cibertec.gestion.repositories.GiroRepository;
import com.cibertec.gestion.repositories.PuestoRepository;
import com.cibertec.gestion.repositories.SocioRepository;
import com.cibertec.gestion.services.IPuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PuestoService implements IPuestoService {

    private final PuestoRepository puestoRepository;
    private final SocioRepository socioRepository;
    private final GiroRepository giroRepository;

    @Override
    public List<PuestoEntity> getAll() {
        return puestoRepository.findAll();
    }

    @Override
    public PuestoEntity getById(Long id) {
        return puestoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Puesto no encontrado"));
    }

    @Override
    public PuestoEntity create(PuestoRequest request) {
        if (puestoRepository.existsByNumero(request.getNumero())) {
            throw new ConflictException("Ya existe un puesto con el número indicado");
        }
        return puestoRepository.save(map(new PuestoEntity(), request));
    }

    @Override
    public PuestoEntity update(Long id, PuestoRequest request) {
        PuestoEntity puesto = getById(id);
        if (puestoRepository.existsByNumeroAndIdNot(request.getNumero(), id)) {
            throw new ConflictException("Ya existe otro puesto con el número indicado");
        }
        return puestoRepository.save(map(puesto, request));
    }

    @Override
    public void delete(Long id) {
        puestoRepository.delete(getById(id));
    }

    private PuestoEntity map(PuestoEntity puesto, PuestoRequest request) {
        if (request.getFechaInicio() != null && request.getFechaFin() != null
                && request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new BusinessException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        SocioEntity socio = request.getSocioId() == null ? null
                : socioRepository.findById(request.getSocioId())
                .orElseThrow(() -> new NotFoundException("Socio no encontrado"));
        GiroEntity giro = giroRepository.findById(request.getGiroId())
                .orElseThrow(() -> new NotFoundException("Giro no encontrado"));

        puesto.setNumero(request.getNumero().trim());
        puesto.setNombreInquilino(request.getNombreInquilino());
        puesto.setFechaInicio(request.getFechaInicio());
        puesto.setFechaFin(request.getFechaFin());
        puesto.setSocio(socio);
        puesto.setGiro(giro);
        puesto.setActivo(request.isActivo());
        return puesto;
    }
}
