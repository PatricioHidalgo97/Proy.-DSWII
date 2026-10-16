package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.EgresoEntity;
import com.cibertec.gestion.models.EgresoRequest;
import com.cibertec.gestion.services.IEgresoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/egresos")
@RequiredArgsConstructor
public class EgresoController {
    private final IEgresoService service;
    @GetMapping public ResponseEntity<List<EgresoEntity>> getByMonth(@RequestParam YearMonth periodo) {
        return ResponseEntity.ok(service.getByMonth(periodo));
    }
    @GetMapping("/{id}") public ResponseEntity<EgresoEntity> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }
    @PostMapping public ResponseEntity<EgresoEntity> create(@Valid @RequestBody EgresoRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, auth.getName()));
    }
    @PutMapping("/{id}/procesar") public ResponseEntity<EgresoEntity> process(@PathVariable Long id) { return ResponseEntity.ok(service.process(id)); }
    @PutMapping("/{id}/anular") public ResponseEntity<EgresoEntity> annul(@PathVariable Long id) { return ResponseEntity.ok(service.annul(id)); }
    @PostMapping(value = "/importar", consumes = "multipart/form-data")
    public ResponseEntity<List<EgresoEntity>> importCsv(@RequestParam("archivo") MultipartFile file, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importCsv(file, auth.getName()));
    }
}
