package com.cibertec.gestion.controllers;

import com.cibertec.gestion.entities.CuentaCobrarEntity;
import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.models.CuentaRequest;
import com.cibertec.gestion.models.CanjeRequest;
import com.cibertec.gestion.models.GenerarCuentasRequest;
import com.cibertec.gestion.models.OperacionLoteRequest;
import com.cibertec.gestion.services.ICuentaCobrarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaCobrarController {
    private final ICuentaCobrarService cuentaService;

    @GetMapping
    public ResponseEntity<List<CuentaCobrarEntity>> getAll() {
        return ResponseEntity.ok(cuentaService.getAll());
    }

    @GetMapping("/puesto/{puestoId}")
    public ResponseEntity<List<CuentaCobrarEntity>> getByPuesto(@PathVariable Long puestoId) {
        return ResponseEntity.ok(cuentaService.getByPuesto(puestoId));
    }

    @GetMapping("/socio/{socioId}")
    public ResponseEntity<List<CuentaCobrarEntity>> getBySocio(@PathVariable Long socioId) {
        return ResponseEntity.ok(cuentaService.getBySocio(socioId));
    }

    @PostMapping
    public ResponseEntity<CuentaCobrarEntity> create(@Valid @RequestBody CuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.create(request));
    }

    @PostMapping("/generar/puestos")
    public ResponseEntity<List<CuentaCobrarEntity>> generateForPuestos(
            @Valid @RequestBody GenerarCuentasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.generateForPuestos(request));
    }

    @PostMapping("/generar/socios")
    public ResponseEntity<List<CuentaCobrarEntity>> generateForSocios(
            @Valid @RequestBody GenerarCuentasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.generateForSocios(request));
    }

    @PutMapping("/{id}/exonerar")
    public ResponseEntity<CuentaCobrarEntity> exonerate(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.exonerate(id));
    }

    @PutMapping("/exonerar-lote")
    public ResponseEntity<List<CuentaCobrarEntity>> exonerateBatch(
            @Valid @RequestBody OperacionLoteRequest request) {
        return ResponseEntity.ok(cuentaService.exonerateBatch(request.getCuentaIds()));
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<ReciboEntity> pay(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.pay(id, authentication.getName()));
    }

    @PostMapping("/pagar-lote")
    public ResponseEntity<List<ReciboEntity>> payBatch(
            @Valid @RequestBody OperacionLoteRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.payBatch(request.getCuentaIds(), authentication.getName()));
    }

    @PostMapping("/canjear")
    public ResponseEntity<ReciboEntity> exchange(
            @Valid @RequestBody CanjeRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cuentaService.exchange(request, authentication.getName()));
    }
}
