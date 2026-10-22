package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.PuestoRequest;
import com.cibertec.gestion.catalog.dto.PuestoResponse;
import com.cibertec.gestion.catalog.entities.GiroEntity;
import com.cibertec.gestion.catalog.entities.PuestoEntity;
import com.cibertec.gestion.catalog.entities.SocioEntity;
import com.cibertec.gestion.catalog.exceptions.BusinessException;
import com.cibertec.gestion.catalog.exceptions.ConflictException;
import com.cibertec.gestion.catalog.exceptions.NotFoundException;
import com.cibertec.gestion.catalog.repositories.GiroRepository;
import com.cibertec.gestion.catalog.repositories.PuestoRepository;
import com.cibertec.gestion.catalog.repositories.SocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PuestoService implements IPuestoService {

    private final PuestoRepository puestoRepository;
    private final SocioRepository socioRepository;
    private final GiroRepository giroRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PuestoResponse> getAll() {
        return puestoRepository.findAll().stream().map(PuestoService::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PuestoResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public PuestoResponse create(PuestoRequest request) {
        if (puestoRepository.existsByNumero(request.getNumero())) {
            throw new ConflictException("Ya existe un puesto con el número indicado");
        }
        return toResponse(puestoRepository.save(map(new PuestoEntity(), request)));
    }

    @Override
    @Transactional
    public PuestoResponse update(Long id, PuestoRequest request) {
        PuestoEntity puesto = findEntity(id);
        if (puestoRepository.existsByNumeroAndIdNot(request.getNumero(), id)) {
            throw new ConflictException("Ya existe otro puesto con el número indicado");
        }
        return toResponse(puestoRepository.save(map(puesto, request)));
    }

    @Override
    public void delete(Long id) {
        puestoRepository.delete(findEntity(id));
    }

    private PuestoEntity findEntity(Long id) {
        return puestoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Puesto no encontrado"));
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

    /**
     * Construye la forma de ESP-04 §5.3. Los campos anidados existen siempre:
     * `socio` se serializa como `null` cuando el puesto no lo tiene.
     */
    static PuestoResponse toResponse(PuestoEntity puesto) {
        SocioEntity socio = puesto.getSocio();
        GiroEntity giro = puesto.getGiro();
        return new PuestoResponse(
                puesto.getId(),
                puesto.getNumero(),
                puesto.getNombreInquilino(),
                puesto.getFechaInicio(),
                puesto.getFechaFin(),
                socio == null ? null : new PuestoResponse.SocioAnidado(
                        socio.getId(),
                        socio.getCodigo(),
                        socio.getNombres(),
                        socio.getApellidos(),
                        socio.getAccion(),
                        socio.getEtapa(),
                        socio.getFechaNacimiento(),
                        socio.isActivo()),
                giro == null ? null : new PuestoResponse.GiroAnidado(
                        giro.getId(),
                        giro.getNombre(),
                        giro.getDescripcion(),
                        giro.isActivo()),
                puesto.isActivo()
        );
    }
}
