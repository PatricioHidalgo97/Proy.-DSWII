package com.cibertec.gestion.controllers;

import com.cibertec.gestion.services.IReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final IReporteService service;

    @GetMapping("/movimientos-diarios")
    public ResponseEntity<byte[]> daily(@RequestParam LocalDate fecha) {
        return file(service.movimientosDiarios(fecha), "movimientos-" + fecha + ".xlsx");
    }
    @GetMapping("/movimientos-mensuales")
    public ResponseEntity<byte[]> monthly(@RequestParam YearMonth periodo) {
        return file(service.movimientosMensuales(periodo), "movimientos-" + periodo + ".xlsx");
    }
    @GetMapping("/socios") public ResponseEntity<byte[]> partners() { return file(service.socios(), "socios.xlsx"); }
    @GetMapping("/egresos") public ResponseEntity<byte[]> expenses(@RequestParam YearMonth periodo) {
        return file(service.egresos(periodo), "egresos-" + periodo + ".xlsx");
    }
    @GetMapping("/bancos") public ResponseEntity<byte[]> banks() { return file(service.bancos(), "bancos.xlsx"); }

    private ResponseEntity<byte[]> file(byte[] bytes, String filename) {
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(bytes);
    }
}
