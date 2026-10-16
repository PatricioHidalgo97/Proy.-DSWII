package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.GiroEntity;
import com.cibertec.gestion.models.GiroRequest;
import com.cibertec.gestion.services.IGiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/giros")
@RequiredArgsConstructor
public class GiroController {
    private final IGiroService giroService;

    @GetMapping
    public ResponseEntity<List<GiroEntity>> getAll() {
        return ResponseEntity.ok(giroService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GiroEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(giroService.getById(id));
    }

    @PostMapping
    public ResponseEntity<GiroEntity> create(@Valid @RequestBody GiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(giroService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GiroEntity> update(@PathVariable Long id, @Valid @RequestBody GiroRequest request) {
        return ResponseEntity.ok(giroService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        giroService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
