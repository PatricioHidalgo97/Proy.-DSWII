package com.cibertec.gestion.catalog.controllers;

import com.cibertec.gestion.catalog.dto.GiroRequest;
import com.cibertec.gestion.catalog.entities.GiroEntity;
import com.cibertec.gestion.catalog.services.IGiroService;
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
@RequestMapping("/api/giros")
@RequiredArgsConstructor
@Tag(name = "Giros", description = "CRUD de giros (ESP-04 §4.2)")
@SecurityRequirement(name = "bearerAuth")
public class GiroController {
    private final IGiroService giroService;

    @GetMapping
    @Operation(summary = "Lista todos los giros")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de giros"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<GiroEntity>> getAll() {
        return ResponseEntity.ok(giroService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un giro por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Giro encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Giro no encontrado")
    })
    public ResponseEntity<GiroEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(giroService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un giro", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Giro creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 400,
                                      "message": "Revise los datos enviados",
                                      "errors": { "nombre": "El nombre es obligatorio" }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 409,
                                      "message": "Ya existe un giro con el nombre indicado"
                                    }
                                    """)))
    })
    public ResponseEntity<GiroEntity> create(@Valid @RequestBody GiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(giroService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un giro", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Giro actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Giro no encontrado"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<GiroEntity> update(@PathVariable Long id, @Valid @RequestBody GiroRequest request) {
        return ResponseEntity.ok(giroService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un giro", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Giro eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Giro no encontrado"),
            @ApiResponse(responseCode = "409", description = "El giro está relacionado con otros datos")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        giroService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
