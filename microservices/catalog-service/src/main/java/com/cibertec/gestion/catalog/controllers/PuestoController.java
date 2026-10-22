package com.cibertec.gestion.catalog.controllers;

import com.cibertec.gestion.catalog.dto.PuestoRequest;
import com.cibertec.gestion.catalog.dto.PuestoResponse;
import com.cibertec.gestion.catalog.services.IPuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/puestos")
@RequiredArgsConstructor
@Tag(name = "Puestos", description = "CRUD de puestos con socio y giro anidados (ESP-04 §4.2, §5.3)")
@SecurityRequirement(name = "bearerAuth")
public class PuestoController {
    private final IPuestoService puestoService;

    @GetMapping
    @Operation(summary = "Lista todos los puestos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de puestos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<PuestoResponse>> getAll() {
        return ResponseEntity.ok(puestoService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un puesto por id",
            description = "Incluye `socio` y `giro` completos; `socio` es `null` si el puesto no tiene socio (ESP-04 §5.3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puesto encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PuestoResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "numero": "P-101",
                                      "nombreInquilino": "Rosa Quispe Mamani",
                                      "fechaInicio": "2024-01-01",
                                      "fechaFin": null,
                                      "socio": {
                                        "id": 1,
                                        "codigo": "SOC-001",
                                        "nombres": "Rosa",
                                        "apellidos": "Quispe Mamani",
                                        "accion": "A-001",
                                        "etapa": 1,
                                        "fechaNacimiento": "1978-03-12",
                                        "activo": true
                                      },
                                      "giro": {
                                        "id": 1,
                                        "nombre": "Abarrotes y víveres",
                                        "descripcion": "Venta de productos de primera necesidad",
                                        "activo": true
                                      },
                                      "activo": true
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado")
    })
    public ResponseEntity<PuestoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(puestoService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un puesto", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Puesto creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha de fin anterior a la de inicio",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 400,
                                      "message": "La fecha de fin no puede ser anterior a la fecha de inicio"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Socio o giro no encontrado"),
            @ApiResponse(responseCode = "409", description = "Número duplicado",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 409,
                                      "message": "Ya existe un puesto con el número indicado"
                                    }
                                    """)))
    })
    public ResponseEntity<PuestoResponse> create(@Valid @RequestBody PuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puestoService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un puesto", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Puesto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha de fin anterior a la de inicio"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Puesto, socio o giro no encontrado"),
            @ApiResponse(responseCode = "409", description = "Número duplicado")
    })
    public ResponseEntity<PuestoResponse> update(@PathVariable Long id, @Valid @RequestBody PuestoRequest request) {
        return ResponseEntity.ok(puestoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un puesto", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Puesto eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Puesto no encontrado"),
            @ApiResponse(responseCode = "409", description = "El puesto está relacionado con otros datos")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        puestoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
