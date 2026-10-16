package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.ServicioEntity;
import com.cibertec.gestion.models.ServicioRequest;
import com.cibertec.gestion.services.IServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {
    private final IServicioService servicioService;

    @GetMapping
    public ResponseEntity<List<ServicioEntity>> getAll() {
        return ResponseEntity.ok(servicioService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServicioEntity> create(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioEntity> update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        servicioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
