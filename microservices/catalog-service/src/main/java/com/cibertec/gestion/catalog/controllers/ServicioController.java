package com.cibertec.gestion.catalog.controllers;

import com.cibertec.gestion.catalog.dto.ServicioRequest;
import com.cibertec.gestion.catalog.entities.ServicioEntity;
import com.cibertec.gestion.catalog.services.IServicioService;
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
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "CRUD de servicios de puesto y de socio (ESP-04 §4.2)")
@SecurityRequirement(name = "bearerAuth")
public class ServicioController {
    private final IServicioService servicioService;

    @GetMapping
    @Operation(summary = "Lista todos los servicios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de servicios"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<ServicioEntity>> getAll() {
        return ResponseEntity.ok(servicioService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un servicio por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    public ResponseEntity<ServicioEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un servicio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-10-21T10:15:30.123",
                                      "status": 400,
                                      "message": "Revise los datos enviados",
                                      "errors": { "costo": "El costo debe ser mayor a cero" }
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
                                      "message": "Ya existe un servicio con el nombre indicado"
                                    }
                                    """)))
    })
    public ResponseEntity<ServicioEntity> create(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un servicio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    public ResponseEntity<ServicioEntity> update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un servicio", description = "Requiere rol ADMIN (ESP-05 §5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Servicio eliminado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado"),
            @ApiResponse(responseCode = "409", description = "El servicio está relacionado con otros datos")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        servicioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
