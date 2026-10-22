package com.cibertec.gestion.cash.controllers;

import com.cibertec.gestion.cash.dto.EgresoRequest;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import com.cibertec.gestion.cash.services.IEgresoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

/**
 * Cinco operaciones de egresos más la importación CSV (ESP-04 §4.5, §5.6;
 * ESP-08 §7.3). La anulación exige {@code ADMIN} y se resuelve en la cadena de
 * seguridad del servicio (ESP-05 §5).
 */
@RestController
@RequestMapping("/api/egresos")
@RequiredArgsConstructor
@Tag(name = "Egresos", description = "Egresos de tesorería e importación CSV (ESP-04 §4.5)")
@SecurityRequirement(name = "bearerAuth")
public class EgresoController {

    private final IEgresoService egresoService;

    @GetMapping
    @Operation(operationId = "listarEgresos", summary = "Egresos de un mes",
            description = "Listado por periodo `YYYY-MM`, ordenado por fecha descendente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Egresos del mes"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<IEgresoService.EgresoResponse>> getByMonth(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return ResponseEntity.ok(egresoService.getByMonth(periodo));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "obtenerEgreso", summary = "Obtiene un egreso con detalle",
            description = "Devuelve `usuario` construido desde el snapshot, sin consultar identidad (ESP-03 §4.1).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Egreso encontrado",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "id": 25,
                              "comprobante": "EGR-000025",
                              "tipoDocumento": "FACTURA",
                              "numeroDocumento": "F001-900",
                              "proveedor": "Proveedor Demo",
                              "fecha": "2026-10-21",
                              "subtotal": 100.00,
                              "impuesto": 18.00,
                              "total": 118.00,
                              "motivo": "Prueba",
                              "documentoAsociado": "OC-900",
                              "estado": "REGISTRADO",
                              "usuario": {
                                "id": 1,
                                "usuario": "admin",
                                "nombres": "Administrador",
                                "apellidos": "General",
                                "rol": "ADMIN"
                              },
                              "fechaRegistro": "2026-10-22T09:07:00",
                              "fechaActualizacion": null
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    })
    public ResponseEntity<IEgresoService.EgresoResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(egresoService.getById(id));
    }

    @PostMapping
    @Operation(operationId = "crearEgreso", summary = "Registra un egreso",
            description = "El comprobante sale del correlativo `EGRESO` con prefijo `EGR` (ESP-06 §5.7).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Egreso registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<IEgresoService.EgresoResponse> create(
            @Valid @RequestBody EgresoRequest request,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(egresoService.create(request, usuario));
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(operationId = "importarEgresos", summary = "Importa egresos desde un CSV",
            description = """
                    Archivo de texto con separador `;`, cabecera opcional y ocho columnas obligatorias:
                    `tipoDocumento;numeroDocumento;proveedor;fecha;subtotal;impuesto;total;motivo`, más una
                    novena opcional `documentoAsociado`. Tamaño máximo 2 MB. Cada fila genera un comprobante
                    correlativo; un error devuelve 400 con el número de fila (ESP-04 §6, ESP-08 §7.3).""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Egresos importados"),
            @ApiResponse(responseCode = "400", description = "CSV inválido, vacío o con filas incompletas"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<List<IEgresoService.EgresoResponse>> importCsv(
            @Parameter(description = "Archivo CSV separado por punto y coma", required = true)
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(egresoService.importCsv(archivo, usuario));
    }

    @PutMapping("/{id}/procesar")
    @Operation(operationId = "procesarEgreso", summary = "Procesa un egreso",
            description = "Un egreso anulado no puede procesarse (400). Escribe `fechaActualizacion`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Egreso procesado"),
            @ApiResponse(responseCode = "400", description = "El egreso está anulado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    })
    public ResponseEntity<IEgresoService.EgresoResponse> process(@PathVariable Long id) {
        return ResponseEntity.ok(egresoService.process(id));
    }

    @PutMapping("/{id}/anular")
    @Operation(operationId = "anularEgreso", summary = "Anula un egreso",
            description = "Requiere rol `ADMIN` (ESP-05 §5). Un egreso anulado no se anula dos veces (400).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Egreso anulado"),
            @ApiResponse(responseCode = "400", description = "El egreso ya está anulado"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente"),
            @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    })
    public ResponseEntity<IEgresoService.EgresoResponse> annul(@PathVariable Long id) {
        return ResponseEntity.ok(egresoService.annul(id));
    }
}
