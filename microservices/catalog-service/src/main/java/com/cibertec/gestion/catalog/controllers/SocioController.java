package com.cibertec.gestion.catalog.controllers;

import com.cibertec.gestion.catalog.dto.SocioRequest;
import com.cibertec.gestion.catalog.entities.SocioEntity;
import com.cibertec.gestion.catalog.services.ISocioService;
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
@RequestMapping("/api/socios")
@RequiredArgsConstructor
@Tag(name = "Socios", description = "CRUD de socios (ESP-04 §4.2)")
@SecurityRequirement(name = "bearerAuth")
public class SocioController {

    private final ISocioService socioService;

    @GetMapping
    @Operation(summary = "Lista todos los socios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de socios"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido",
                    content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<SocioEntity>> getAll() {
        return ResponseEntity.ok(socioService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un socio por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Socio encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Socio no encontrado")
    })
    public ResponseEntity<SocioEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(socioService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un socio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Socio creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 400,
                                      "message": "Revise los datos enviados",
                                      "errors": { "codigo": "El código es obligatorio" }
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "409", description = "Código duplicado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 409,
                                      "message": "Ya existe un socio con el código indicado"
                                    }
                                    """)))
    })
    public ResponseEntity<SocioEntity> create(@Valid @RequestBody SocioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(socioService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un socio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Socio actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Socio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Código duplicado")
    })
    public ResponseEntity<SocioEntity> update(@PathVariable Long id, @Valid @RequestBody SocioRequest request) {
        return ResponseEntity.ok(socioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un socio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Socio eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Socio no encontrado"),
            @ApiResponse(responseCode = "409", description = "El socio está relacionado con otros datos")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        socioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
