package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.enums.TipoRecibo;
import com.cibertec.gestion.models.IngresoExternoRequest;
import com.cibertec.gestion.services.IReciboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
public class ReciboController {
    private final IReciboService reciboService;

    @GetMapping("/{id}")
    public ResponseEntity<ReciboEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reciboService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReciboEntity>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) TipoRecibo tipo) {
        return ResponseEntity.ok(reciboService.getByDate(fecha, tipo));
    }

    @PostMapping("/externos")
    public ResponseEntity<ReciboEntity> createExternalIncome(
            @Valid @RequestBody IngresoExternoRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reciboService.createExternalIncome(request, authentication.getName()));
    }
}
