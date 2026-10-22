package com.cibertec.gestion.catalog.services;

import com.cibertec.gestion.catalog.dto.ServicioRequest;
import com.cibertec.gestion.catalog.entities.ServicioEntity;
import com.cibertec.gestion.catalog.exceptions.ConflictException;
import com.cibertec.gestion.catalog.exceptions.NotFoundException;
import com.cibertec.gestion.catalog.repositories.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService implements IServicioService {

    private final ServicioRepository servicioRepository;

    @Override
    public List<ServicioEntity> getAll() {
        return servicioRepository.findAll();
    }

    @Override
    public ServicioEntity getById(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
    }

    @Override
    public ServicioEntity create(ServicioRequest request) {
        if (servicioRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ConflictException("Ya existe un servicio con el nombre indicado");
        }
        return servicioRepository.save(map(new ServicioEntity(), request));
    }

    @Override
    public ServicioEntity update(Long id, ServicioRequest request) {
        ServicioEntity servicio = getById(id);
        if (servicioRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre(), id)) {
            throw new ConflictException("Ya existe otro servicio con el nombre indicado");
        }
        return servicioRepository.save(map(servicio, request));
    }

    @Override
    public void delete(Long id) {
        servicioRepository.delete(getById(id));
    }

    private ServicioEntity map(ServicioEntity servicio, ServicioRequest request) {
        servicio.setNombre(request.getNombre().trim());
        servicio.setRecurrencia(request.getRecurrencia());
        servicio.setCosto(request.getCosto());
        servicio.setMoneda(request.getMoneda());
        servicio.setCargoA(request.getCargoA());
        servicio.setDependeConsumo(request.isDependeConsumo());
        servicio.setActivo(request.isActivo());
        return servicio;
    }
}
