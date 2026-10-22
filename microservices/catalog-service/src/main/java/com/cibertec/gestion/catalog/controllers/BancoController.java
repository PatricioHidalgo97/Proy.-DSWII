package com.cibertec.gestion.catalog.controllers;

import com.cibertec.gestion.catalog.dto.BancoRequest;
import com.cibertec.gestion.catalog.entities.BancoEntity;
import com.cibertec.gestion.catalog.services.IBancoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bancos")
@RequiredArgsConstructor
@Tag(name = "Bancos", description = "CRUD de cuentas bancarias (ESP-04 §4.2)")
@SecurityRequirement(name = "bearerAuth")
public class BancoController {
    private final IBancoService service;

    @GetMapping
    @Operation(summary = "Lista todos los bancos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de bancos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<BancoEntity>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un banco por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banco encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Banco no encontrado")
    })
    public ResponseEntity<BancoEntity> getById(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    @Operation(summary = "Crea un banco", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Banco creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 400,
                                      "message": "Revise los datos enviados",
                                      "errors": { "numeroCuenta": "El número de cuenta es obligatorio" }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "409", description = "Cuenta o CCI duplicado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 409,
                                      "message": "Ya existe la cuenta bancaria indicada"
                                    }
                                    """)))
    })
    public ResponseEntity<BancoEntity> create(@Valid @RequestBody BancoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un banco", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Banco actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Banco no encontrado"),
            @ApiResponse(responseCode = "409", description = "Cuenta o CCI duplicado")
    })
    public ResponseEntity<BancoEntity> update(@PathVariable Long id, @Valid @RequestBody BancoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un banco", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Banco eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Banco no encontrado"),
            @ApiResponse(responseCode = "409", description = "El banco está relacionado con otros datos")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}
