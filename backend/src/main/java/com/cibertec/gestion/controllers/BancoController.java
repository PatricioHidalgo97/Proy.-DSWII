package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.BancoEntity;
import com.cibertec.gestion.models.BancoRequest;
import com.cibertec.gestion.services.IBancoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bancos")
@RequiredArgsConstructor
public class BancoController {
    private final IBancoService service;
    @GetMapping public ResponseEntity<List<BancoEntity>> getAll() { return ResponseEntity.ok(service.getAll()); }
    @GetMapping("/{id}") public ResponseEntity<BancoEntity> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }
    @PostMapping public ResponseEntity<BancoEntity> create(@Valid @RequestBody BancoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
    @PutMapping("/{id}") public ResponseEntity<BancoEntity> update(@PathVariable Long id, @Valid @RequestBody BancoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}
