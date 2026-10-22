package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.clients.CatalogoClient;
import com.cibertec.gestion.cash.clients.dto.BancoRemoto;
import com.cibertec.gestion.cash.clients.dto.SocioRemoto;
import com.cibertec.gestion.cash.entities.CorrelativoEntity;
import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;
import com.cibertec.gestion.cash.entities.EgresoEntity;
import com.cibertec.gestion.cash.entities.ReciboEntity;
import com.cibertec.gestion.cash.enums.EstadoCuenta;
import com.cibertec.gestion.cash.enums.EstadoEgreso;
import com.cibertec.gestion.cash.enums.TipoRecibo;
import com.cibertec.gestion.cash.repositories.CorrelativoRepository;
import com.cibertec.gestion.cash.repositories.CuentaCobrarRepository;
import com.cibertec.gestion.cash.repositories.EgresoRepository;
import com.cibertec.gestion.cash.repositories.ReciboRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Reportes XLSX (ESP-04 §4.6, ESP-13 §5.1). Cada uno debe devolver un libro
 * no vacío y conservar hojas y encabezados; socios y bancos se alimentan del
 * catálogo mockeado, y el destinatario de un recibo sale del snapshot.
 */
@SpringBootTest
class ReporteServiceTest {

    private static final String PERIODO = "2026-11";

    @Autowired
    private IReporteService reporteService;

    @Autowired
    private ReciboRepository reciboRepository;

    @Autowired
    private CuentaCobrarRepository cuentaRepository;

    @Autowired
    private EgresoRepository egresoRepository;

    @Autowired
    private CorrelativoRepository correlativoRepository;

    @MockitoBean
    private CatalogoClient catalogoClient;

    @BeforeEach
    void prepareData() {
        reciboRepository.deleteAll();
        cuentaRepository.deleteAll();
        egresoRepository.deleteAll();
        if (correlativoRepository.findById("RECIBO").isEmpty()) {
            correlativoRepository.save(new CorrelativoEntity("RECIBO", 0L));
        }
        if (correlativoRepository.findById("EGRESO").isEmpty()) {
            correlativoRepository.save(new CorrelativoEntity("EGRESO", 0L));
        }
        when(catalogoClient.listSocios()).thenReturn(List.of(
                new SocioRemoto(1L, "SOC-001", "Rosa", "Quispe Mamani", "A-001", 1, true)));
        when(catalogoClient.listBancos()).thenReturn(List.of(
                new BancoRemoto(1L, "BCP", "193-000001", "00219300000123456789", "PEN", true)));
    }

    @Test
    void movimientosDiariosDebeTraerLasDosHojas() throws Exception {
        sembrarCuentaConRecibo();
        sembrarEgreso();

        byte[] bytes = reporteService.movimientosDiarios(LocalDate.of(2026, 11, 10));

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheet("Ingresos")).isNotNull();
            assertThat(wb.getSheet("Egresos")).isNotNull();
            assertThat(wb.getSheet("Ingresos").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Correlativo");
            assertThat(wb.getSheet("Egresos").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Comprobante");
            assertThat(wb.getSheet("Ingresos").getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void movimientosMensualesDebeTraerLasDosHojas() throws Exception {
        sembrarCuentaConRecibo();
        sembrarEgreso();

        byte[] bytes = reporteService.movimientosMensuales(YearMonth.of(2026, 11));

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheet("Egresos").getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void sociosDebeTraerUnaFilaPorSocio() throws Exception {
        byte[] bytes = reporteService.socios();

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Socios");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Código");
            Row fila = sheet.getRow(1);
            assertThat(fila.getCell(0).getStringCellValue()).isEqualTo("SOC-001");
            assertThat(fila.getCell(3).getStringCellValue()).isEqualTo("A-001");
        }
    }

    @Test
    void egresosDebeTraerElComprobante() throws Exception {
        sembrarEgreso();

        byte[] bytes = reporteService.egresos(YearMonth.of(2026, 11));

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Egresos");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Comprobante");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).startsWith("EGR-");
            assertThat(sheet.getRow(1).getCell(9).getStringCellValue()).isEqualTo("REGISTRADO");
        }
    }

    @Test
    void bancosDebeTraerUnaFilaPorBanco() throws Exception {
        byte[] bytes = reporteService.bancos();

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Bancos");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Banco");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("BCP");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("PEN");
        }
    }

    /**
     * El destinatario del recibo sale del snapshot de la cuenta, no de una
     * relación JPA hacia catálogo (ESP-03 §4.2).
     */
    @Test
    void elDestinatarioDelReciboDebeSalirDelSnapshot() throws Exception {
        sembrarCuentaConRecibo();

        byte[] bytes = reporteService.movimientosMensuales(YearMonth.of(2026, 11));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            // Columnas: Correlativo, Fecha, Tipo, Destinatario, ...
            assertThat(wb.getSheet("Ingresos").getRow(1).getCell(3).getStringCellValue())
                    .isEqualTo("Rosa Quispe Mamani");
        }
    }

    // --- utilidades -------------------------------------------------------

    private void sembrarCuentaConRecibo() {
        CuentaCobrarEntity cuenta = cuentaRepository.save(CuentaCobrarEntity.builder()
                .periodo(PERIODO)
                .monto(new BigDecimal("50.00"))
                .estado(EstadoCuenta.PAGADA)
                .fechaCreacion(LocalDateTime.of(2026, 11, 1, 8, 0))
                .socioId(1L)
                .servicioId(1L)
                .servicioNombre("Cuota ordinaria de socio")
                .servicioMoneda("PEN")
                .servicioCargoA("SOCIO")
                .servicioDependeConsumo(false)
                .socioCodigo("SOC-001")
                .socioNombres("Rosa")
                .socioApellidos("Quispe Mamani")
                .build());
        reciboRepository.save(ReciboEntity.builder()
                .correlativo("REC-900001")
                .fecha(LocalDateTime.of(2026, 11, 10, 10, 0))
                .monto(new BigDecimal("50.00"))
                .tipo(TipoRecibo.INGRESO)
                .cuentaCobrar(cuenta)
                .concepto("Pago de cuota")
                .usuarioId(1L)
                .usuarioUsuario("admin")
                .usuarioNombres("Administrador")
                .usuarioApellidos("General")
                .usuarioRol("ADMIN")
                .build());
    }

    private void sembrarEgreso() {
        egresoRepository.save(EgresoEntity.builder()
                .comprobante("EGR-900001")
                .tipoDocumento("FACTURA")
                .numeroDocumento("F001-900")
                .proveedor("Proveedor Demo")
                .fecha(LocalDate.of(2026, 11, 10))
                .subtotal(new BigDecimal("100.00"))
                .impuesto(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .motivo("Prueba")
                .estado(EstadoEgreso.REGISTRADO)
                .usuarioId(1L)
                .usuarioUsuario("admin")
                .usuarioNombres("Administrador")
                .usuarioApellidos("General")
                .usuarioRol("ADMIN")
                .fechaRegistro(LocalDateTime.of(2026, 11, 10, 9, 0))
                .build());
    }
}
