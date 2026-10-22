package com.cibertec.gestion.cash.controllers;

import com.cibertec.gestion.cash.dto.IngresoExternoRequest;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import com.cibertec.gestion.cash.services.IReciboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Tres operaciones de recibos (ESP-04 §4.4, §5.5). */
@RestController
@RequestMapping("/api/recibos")
@RequiredArgsConstructor
@Tag(name = "Recibos", description = "Consulta de recibos e ingresos externos (ESP-04 §4.4)")
@SecurityRequirement(name = "bearerAuth")
public class ReciboController {

    private final IReciboService reciboService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un recibo con detalle",
            description = "Devuelve `cuentaCobrar`, `banco` y `usuario` construidos desde los snapshots.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recibo encontrado",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "id": 1,
                              "correlativo": "REC-000001",
                              "fecha": "2026-08-06T10:36:00",
                              "monto": 50.00,
                              "tipo": "BANCARIO",
                              "cuentaCobrar": {
                                "id": 96,
                                "periodo": "2026-08",
                                "monto": 50.00,
                                "estado": "PAGADA",
                                "servicio": { "id": 6, "nombre": "Cuota ordinaria de socio" },
                                "socio": { "id": 1, "codigo": "SOC-001", "nombres": "Rosa", "apellidos": "Quispe Mamani" },
                                "puesto": null
                              },
                              "banco": {
                                "id": 2,
                                "nombre": "BBVA Perú",
                                "numeroCuenta": "0011-0455-0100234567",
                                "cci": "01104550010023456718",
                                "moneda": "PEN",
                                "activo": true
                              },
                              "depositante": null,
                              "categoria": null,
                              "concepto": "Canje bancario académico",
                              "fechaDeposito": "2026-08-06",
                              "usuario": {
                                "id": 3,
                                "usuario": "caja2",
                                "nombres": "Lucía",
                                "apellidos": "Operadora",
                                "rol": "OPERADOR"
                              }
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Recibo no encontrado")
    })
    public ResponseEntity<ReciboMapper.ReciboResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reciboService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Recibos de una fecha",
            description = "Filtro opcional por `tipo` (`INGRESO`, `INGRESO_EXTERNO`, `BANCARIO`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recibos de la fecha"),
            @ApiResponse(responseCode = "400", description = "Tipo de recibo inválido"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<ReciboMapper.ReciboResponse>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(reciboService.getByDate(fecha, tipo));
    }

    @PostMapping("/externos")
    @Operation(summary = "Registra un ingreso externo",
            description = "No se asocia a una cuenta. El snapshot del usuario sale del JWT (ESP-03 §4.1).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recibo de ingreso externo emitido"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<ReciboMapper.ReciboResponse> createExternalIncome(
            @Valid @RequestBody IngresoExternoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reciboService.createExternalIncome(request, usuario));
    }
}
