package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.SocioEntity;
import com.cibertec.gestion.models.SocioRequest;
import com.cibertec.gestion.services.ISocioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/socios")
@RequiredArgsConstructor
public class SocioController {

    private final ISocioService socioService;

    @GetMapping
    public ResponseEntity<List<SocioEntity>> getAll() {
        return ResponseEntity.ok(socioService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SocioEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(socioService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SocioEntity> create(@Valid @RequestBody SocioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socioService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SocioEntity> update(@PathVariable Long id, @Valid @RequestBody SocioRequest request) {
        return ResponseEntity.ok(socioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        socioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
