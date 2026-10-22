package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.clients.CatalogoClient;
import com.cibertec.gestion.cash.clients.dto.BancoRemoto;
import com.cibertec.gestion.cash.clients.dto.SocioRemoto;
import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;
import com.cibertec.gestion.cash.entities.EgresoEntity;
import com.cibertec.gestion.cash.entities.ReciboEntity;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.repositories.EgresoRepository;
import com.cibertec.gestion.cash.repositories.ReciboRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Reportes XLSX (ESP-04 §4.6, ESP-08 §7.3).
 *
 * <p>Se conservan las hojas, los encabezados, el congelado de paneles, el
 * autofiltro y el autoajuste de columnas del monolito. La diferencia de fondo
 * es la fuente de los datos: socios y bancos se leen por HTTP de catálogo a
 * través de {@link CatalogoClient} (ESP-03 §1, §3), y egresos y recibos se leen
 * de las tablas propias de caja. En {@code writeRecibos} el destinatario sale
 * del snapshot de la cuenta ({@code socio_nombres} / {@code puesto_numero}),
 * nunca de una relación JPA remota (ESP-03 §4.2, §4.3).</p>
 */
@Service
@RequiredArgsConstructor
public class ReporteService implements IReporteService {

    private static final String[] RECIBOS_HEADERS =
            {"Correlativo", "Fecha", "Tipo", "Destinatario", "Concepto", "Banco", "Monto", "Usuario"};
    private static final String[] EGRESOS_HEADERS =
            {"Comprobante", "Fecha", "Documento", "Número", "Proveedor", "Motivo", "Subtotal", "Impuesto", "Total", "Estado"};

    private final ReciboRepository reciboRepository;
    private final EgresoRepository egresoRepository;
    private final CatalogoClient catalogoClient;

    @Override
    @Transactional(readOnly = true)
    public byte[] movimientosDiarios(LocalDate fecha) {
        return movimientos(fecha, fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] movimientosMensuales(YearMonth periodo) {
        return movimientos(periodo.atDay(1), periodo.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] socios() {
        List<SocioRemoto> socios = catalogoClient.listSocios();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Socios");
            String[] headers = {"Código", "Nombres", "Apellidos", "Acción", "Etapa", "Activo"};
            header(sheet, headers);
            int rowNumber = 1;
            for (SocioRemoto socio : socios) {
                Row row = sheet.createRow(rowNumber++);
                values(row, socio.codigo(), socio.nombres(), socio.apellidos(), socio.accion(),
                        socio.etapa(), socio.activo() ? "Sí" : "No");
            }
            finish(sheet, headers.length);
            return bytes(wb);
        } catch (IOException ex) {
            throw reportError(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] egresos(YearMonth periodo) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Egresos");
            writeEgresos(sheet, egresoRepository.findByFechaBetweenOrderByFechaDesc(
                    periodo.atDay(1), periodo.atEndOfMonth()));
            return bytes(wb);
        } catch (IOException ex) {
            throw reportError(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] bancos() {
        List<BancoRemoto> bancos = catalogoClient.listBancos();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Bancos");
            String[] headers = {"Banco", "Número de cuenta", "CCI", "Moneda", "Activo"};
            header(sheet, headers);
            int rowNumber = 1;
            for (BancoRemoto banco : bancos) {
                Row row = sheet.createRow(rowNumber++);
                values(row, banco.nombre(), banco.numeroCuenta(), banco.cci(), banco.moneda(),
                        banco.activo() ? "Sí" : "No");
            }
            finish(sheet, headers.length);
            return bytes(wb);
        } catch (IOException ex) {
            throw reportError(ex);
        }
    }

    /**
     * Movimientos del rango: una hoja {@code Ingresos} con los recibos y otra
     * {@code Egresos}, en ese orden (ESP-04 §4.6).
     */
    private byte[] movimientos(LocalDate inicio, LocalDate fin) {
        LocalDateTime from = inicio.atStartOfDay();
        LocalDateTime to = fin.plusDays(1).atStartOfDay().minusNanos(1);
        try (Workbook wb = new XSSFWorkbook()) {
            writeRecibos(wb.createSheet("Ingresos"),
                    reciboRepository.findByFechaBetweenOrderByFechaDesc(from, to));
            writeEgresos(wb.createSheet("Egresos"),
                    egresoRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin));
            return bytes(wb);
        } catch (IOException ex) {
            throw reportError(ex);
        }
    }

    private void writeRecibos(Sheet sheet, List<ReciboEntity> recibos) {
        header(sheet, RECIBOS_HEADERS);
        int rowNumber = 1;
        for (ReciboEntity recibo : recibos) {
            String destinatario = recibo.getDepositante();
            CuentaCobrarEntity cuenta = recibo.getCuentaCobrar();
            if (cuenta != null) {
                destinatario = destinatarioSnapshot(cuenta);
            }
            Row row = sheet.createRow(rowNumber++);
            values(row, recibo.getCorrelativo(), recibo.getFecha().toString(),
                    recibo.getTipo() == null ? null : recibo.getTipo().name(), destinatario,
                    recibo.getConcepto(), recibo.getBancoNombre(), recibo.getMonto(),
                    recibo.getUsuarioUsuario());
        }
        finish(sheet, RECIBOS_HEADERS.length);
    }

    /**
     * Destinatario desde el snapshot de la cuenta (ESP-03 §4.2, §4.3): el socio
     * si la cuenta es por socio o si el puesto lo tiene; si no, el número de
     * puesto.
     */
    private String destinatarioSnapshot(CuentaCobrarEntity cuenta) {
        if (cuenta.getSocioNombres() != null) {
            String apellidos = cuenta.getSocioApellidos() == null ? "" : " " + cuenta.getSocioApellidos();
            return cuenta.getSocioNombres() + apellidos;
        }
        if (cuenta.getPuestoSocioNombres() != null) {
            String apellidos = cuenta.getPuestoSocioApellidos() == null ? "" : " " + cuenta.getPuestoSocioApellidos();
            return cuenta.getPuestoSocioNombres() + apellidos;
        }
        return "Puesto " + cuenta.getPuestoNumero();
    }

    private void writeEgresos(Sheet sheet, List<EgresoEntity> egresos) {
        header(sheet, EGRESOS_HEADERS);
        int rowNumber = 1;
        for (EgresoEntity egreso : egresos) {
            Row row = sheet.createRow(rowNumber++);
            values(row, egreso.getComprobante(), egreso.getFecha().toString(), egreso.getTipoDocumento(),
                    egreso.getNumeroDocumento(), egreso.getProveedor(), egreso.getMotivo(),
                    egreso.getSubtotal(), egreso.getImpuesto(), egreso.getTotal(),
                    egreso.getEstado().name());
        }
        finish(sheet, EGRESOS_HEADERS.length);
    }

    private void header(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void values(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            Cell cell = row.createCell(i);
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : value.toString());
        }
    }

    private void finish(Sheet sheet, int columns) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, Math.max(0, sheet.getLastRowNum()), 0, columns - 1));
        for (int i = 0; i < columns; i++) sheet.autoSizeColumn(i);
    }

    private byte[] bytes(Workbook wb) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        wb.write(output);
        return output.toByteArray();
    }

    private BusinessException reportError(IOException ex) {
        return new BusinessException("No se pudo generar el reporte: " + ex.getMessage());
    }
}
