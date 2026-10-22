package com.cibertec.gestion.cash.controllers;

import com.cibertec.gestion.cash.services.IReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Los cinco reportes XLSX de ESP-04 §4.6. Todos responden con el MIME de XLSX y
 * {@code Content-Disposition: attachment} con el nombre del archivo
 * (ESP-08 §7.3).
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes XLSX de caja (ESP-04 §4.6)")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IReporteService reporteService;

    @GetMapping("/movimientos-diarios")
    @Operation(summary = "Movimientos de un día",
            description = "Libro XLSX con las hojas `Ingresos` y `Egresos` de la fecha indicada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro XLSX de movimientos del día",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<byte[]> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return file(reporteService.movimientosDiarios(fecha), "movimientos-" + fecha + ".xlsx");
    }

    @GetMapping("/movimientos-mensuales")
    @Operation(summary = "Movimientos de un mes",
            description = "Libro XLSX con las hojas `Ingresos` y `Egresos` del periodo `YYYY-MM`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro XLSX de movimientos del mes",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<byte[]> monthly(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return file(reporteService.movimientosMensuales(periodo), "movimientos-" + periodo + ".xlsx");
    }

    @GetMapping("/socios")
    @Operation(summary = "Reporte de socios",
            description = "Hoja `Socios`; los datos se leen de catalog-service por HTTP (ESP-03 §6).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro XLSX de socios",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible")
    })
    public ResponseEntity<byte[]> partners() {
        return file(reporteService.socios(), "socios.xlsx");
    }

    @GetMapping("/egresos")
    @Operation(summary = "Reporte de egresos de un mes",
            description = "Hoja `Egresos` con los egresos del periodo `YYYY-MM`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro XLSX de egresos",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    })
    public ResponseEntity<byte[]> expenses(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        return file(reporteService.egresos(periodo), "egresos-" + periodo + ".xlsx");
    }

    @GetMapping("/bancos")
    @Operation(summary = "Reporte de bancos",
            description = "Hoja `Bancos`; los datos se leen de catalog-service por HTTP (ESP-03 §6).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro XLSX de bancos",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido"),
            @ApiResponse(responseCode = "503", description = "Catálogo no disponible")
    })
    public ResponseEntity<byte[]> banks() {
        return file(reporteService.bancos(), "bancos.xlsx");
    }

    private ResponseEntity<byte[]> file(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(bytes);
    }
}
