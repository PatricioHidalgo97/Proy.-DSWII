package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.BancoEntity;
import com.cibertec.gestion.exceptions.ConflictException;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.BancoRequest;
import com.cibertec.gestion.repositories.BancoRepository;
import com.cibertec.gestion.services.IBancoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BancoService implements IBancoService {
    private final BancoRepository repository;

    @Override
    public List<BancoEntity> getAll() { return repository.findAll(); }

    @Override
    public BancoEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Banco no encontrado"));
    }

    @Override
    public BancoEntity create(BancoRequest request) {
        if (repository.existsByNumeroCuenta(request.getNumeroCuenta().trim())) {
            throw new ConflictException("Ya existe la cuenta bancaria indicada");
        }
        if (request.getCci() != null && !request.getCci().isBlank()
                && repository.existsByCci(request.getCci().trim())) {
            throw new ConflictException("Ya existe el CCI indicado");
        }
        return repository.save(map(new BancoEntity(), request));
    }

    @Override
    public BancoEntity update(Long id, BancoRequest request) {
        if (repository.existsByNumeroCuentaAndIdNot(request.getNumeroCuenta().trim(), id)) {
            throw new ConflictException("La cuenta bancaria pertenece a otro banco");
        }
        if (request.getCci() != null && !request.getCci().isBlank()
                && repository.existsByCciAndIdNot(request.getCci().trim(), id)) {
            throw new ConflictException("El CCI pertenece a otro banco");
        }
        return repository.save(map(getById(id), request));
    }

    @Override
    public void delete(Long id) { repository.delete(getById(id)); }

    private BancoEntity map(BancoEntity entity, BancoRequest request) {
        entity.setNombre(request.getNombre().trim());
        entity.setNumeroCuenta(request.getNumeroCuenta().trim());
        entity.setCci(request.getCci() == null || request.getCci().isBlank() ? null : request.getCci().trim());
        entity.setMoneda(request.getMoneda());
        entity.setActivo(request.isActivo());
        return entity;
    }
}
