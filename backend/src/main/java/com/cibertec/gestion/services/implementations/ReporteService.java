package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.*;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.repositories.*;
import com.cibertec.gestion.services.IReporteService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService implements IReporteService {
    private final ReciboRepository reciboRepository;
    private final EgresoRepository egresoRepository;
    private final SocioRepository socioRepository;
    private final BancoRepository bancoRepository;

    @Override public byte[] movimientosDiarios(LocalDate fecha) {
        return movimientos(fecha, fecha, "Movimientos " + fecha);
    }

    @Override public byte[] movimientosMensuales(YearMonth periodo) {
        return movimientos(periodo.atDay(1), periodo.atEndOfMonth(), "Movimientos " + periodo);
    }

    @Override public byte[] socios() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Socios");
            String[] headers = {"Código", "Nombres", "Apellidos", "Acción", "Etapa", "Activo"};
            header(sheet, headers);
            int rowNumber = 1;
            for (SocioEntity socio : socioRepository.findAll()) {
                Row row = sheet.createRow(rowNumber++);
                values(row, socio.getCodigo(), socio.getNombres(), socio.getApellidos(), socio.getAccion(),
                        socio.getEtapa(), socio.isActivo() ? "Sí" : "No");
            }
            finish(sheet, headers.length);
            return bytes(wb);
        } catch (IOException ex) { throw reportError(ex); }
    }

    @Override public byte[] egresos(YearMonth periodo) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Egresos");
            writeEgresos(sheet, egresoRepository.findByFechaBetweenOrderByFechaDesc(periodo.atDay(1), periodo.atEndOfMonth()));
            return bytes(wb);
        } catch (IOException ex) { throw reportError(ex); }
    }

    @Override public byte[] bancos() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Bancos");
            String[] headers = {"Banco", "Número de cuenta", "CCI", "Moneda", "Activo"};
            header(sheet, headers);
            int rowNumber = 1;
            for (BancoEntity banco : bancoRepository.findAll()) {
                Row row = sheet.createRow(rowNumber++);
                values(row, banco.getNombre(), banco.getNumeroCuenta(), banco.getCci(), banco.getMoneda().name(),
                        banco.isActivo() ? "Sí" : "No");
            }
            finish(sheet, headers.length);
            return bytes(wb);
        } catch (IOException ex) { throw reportError(ex); }
    }

    private byte[] movimientos(LocalDate inicio, LocalDate fin, String title) {
        LocalDateTime from = inicio.atStartOfDay();
        LocalDateTime to = fin.plusDays(1).atStartOfDay().minusNanos(1);
        try (Workbook wb = new XSSFWorkbook()) {
            writeRecibos(wb.createSheet("Ingresos"), reciboRepository.findByFechaBetweenOrderByFechaDesc(from, to));
            writeEgresos(wb.createSheet("Egresos"), egresoRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin));
            return bytes(wb);
        } catch (IOException ex) { throw reportError(ex); }
    }

    private void writeRecibos(Sheet sheet, List<ReciboEntity> recibos) {
        String[] headers = {"Correlativo", "Fecha", "Tipo", "Destinatario", "Concepto", "Banco", "Monto", "Usuario"};
        header(sheet, headers);
        int rowNumber = 1;
        for (ReciboEntity recibo : recibos) {
            String destinatario = recibo.getDepositante();
            if (recibo.getCuentaCobrar() != null) {
                CuentaCobrarEntity cuenta = recibo.getCuentaCobrar();
                destinatario = cuenta.getSocio() != null
                        ? cuenta.getSocio().getNombres() + " " + cuenta.getSocio().getApellidos()
                        : "Puesto " + cuenta.getPuesto().getNumero();
            }
            Row row = sheet.createRow(rowNumber++);
            values(row, recibo.getCorrelativo(), recibo.getFecha().toString(), recibo.getTipo().name(), destinatario,
                    recibo.getConcepto(), recibo.getBanco() == null ? null : recibo.getBanco().getNombre(),
                    recibo.getMonto(), recibo.getUsuario().getUsuario());
        }
        finish(sheet, headers.length);
    }

    private void writeEgresos(Sheet sheet, List<EgresoEntity> egresos) {
        String[] headers = {"Comprobante", "Fecha", "Documento", "Número", "Proveedor", "Motivo", "Subtotal", "Impuesto", "Total", "Estado"};
        header(sheet, headers);
        int rowNumber = 1;
        for (EgresoEntity egreso : egresos) {
            Row row = sheet.createRow(rowNumber++);
            values(row, egreso.getComprobante(), egreso.getFecha().toString(), egreso.getTipoDocumento(),
                    egreso.getNumeroDocumento(), egreso.getProveedor(), egreso.getMotivo(), egreso.getSubtotal(),
                    egreso.getImpuesto(), egreso.getTotal(), egreso.getEstado().name());
        }
        finish(sheet, headers.length);
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
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, sheet.getLastRowNum()), 0, columns - 1));
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
