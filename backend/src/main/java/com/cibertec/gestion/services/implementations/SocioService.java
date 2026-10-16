package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.SocioEntity;
import com.cibertec.gestion.exceptions.ConflictException;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.SocioRequest;
import com.cibertec.gestion.repositories.SocioRepository;
import com.cibertec.gestion.services.ISocioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocioService implements ISocioService {

    private final SocioRepository socioRepository;

    @Override
    public List<SocioEntity> getAll() {
        return socioRepository.findAll();
    }

    @Override
    public SocioEntity getById(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Socio no encontrado"));
    }

    @Override
    public SocioEntity create(SocioRequest request) {
        if (socioRepository.existsByCodigo(request.getCodigo())) {
            throw new ConflictException("Ya existe un socio con el código indicado");
        }
        return socioRepository.save(map(new SocioEntity(), request));
    }

    @Override
    public SocioEntity update(Long id, SocioRequest request) {
        SocioEntity socio = getById(id);
        if (socioRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
            throw new ConflictException("Ya existe otro socio con el código indicado");
        }
        return socioRepository.save(map(socio, request));
    }

    @Override
    public void delete(Long id) {
        socioRepository.delete(getById(id));
    }

    private SocioEntity map(SocioEntity socio, SocioRequest request) {
        socio.setCodigo(request.getCodigo().trim());
        socio.setNombres(request.getNombres().trim());
        socio.setApellidos(request.getApellidos().trim());
        socio.setAccion(request.getAccion());
        socio.setEtapa(request.getEtapa());
        socio.setFechaNacimiento(request.getFechaNacimiento());
        socio.setActivo(request.isActivo());
        return socio;
    }
}
