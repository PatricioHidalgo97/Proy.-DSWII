package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.PuestoEntity;
import com.cibertec.gestion.models.PuestoRequest;
import com.cibertec.gestion.services.IPuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/puestos")
@RequiredArgsConstructor
public class PuestoController {
    private final IPuestoService puestoService;

    @GetMapping
    public ResponseEntity<List<PuestoEntity>> getAll() {
        return ResponseEntity.ok(puestoService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PuestoEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(puestoService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PuestoEntity> create(@Valid @RequestBody PuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puestoService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PuestoEntity> update(@PathVariable Long id, @Valid @RequestBody PuestoRequest request) {
        return ResponseEntity.ok(puestoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        puestoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
