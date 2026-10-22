package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.EgresoRequest;
import com.cibertec.gestion.cash.entities.EgresoEntity;
import com.cibertec.gestion.cash.enums.EstadoEgreso;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.exceptions.NotFoundException;
import com.cibertec.gestion.cash.repositories.EgresoRepository;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tesorería (ESP-04 §4.5, §5.6; ESP-03 §4.1).
 *
 * <p>Se portan las reglas del monolito sin cambios: un egreso anulado no se
 * procesa, un egreso no se anula dos veces y toda transición escribe
 * {@code fechaActualizacion}. El comprobante sale siempre de
 * {@link CorrelativoService} con prefijo {@code EGR} (ESP-06 §5.7).</p>
 */
@Service
@RequiredArgsConstructor
public class EgresoService implements IEgresoService {

    private static final String TIPO_CORRELATIVO = "EGRESO";
    private static final String PREFIJO_CORRELATIVO = "EGR";

    private final EgresoRepository egresoRepository;
    private final CorrelativoService correlativoService;

    @Override
    public List<EgresoResponse> getByMonth(YearMonth month) {
        return egresoRepository.findByFechaBetweenOrderByFechaDesc(month.atDay(1), month.atEndOfMonth())
                .stream().map(IEgresoService::toResponse).toList();
    }

    @Override
    public EgresoResponse getById(Long id) {
        return IEgresoService.toResponse(find(id));
    }

    @Override
    @Transactional
    public EgresoResponse create(EgresoRequest request, UsuarioPrincipal usuario) {
        validarUsuario(usuario);
        return IEgresoService.toResponse(egresoRepository.save(map(request, usuario)));
    }

    @Override
    @Transactional
    public EgresoResponse process(Long id) {
        EgresoEntity entity = find(id);
        if (entity.getEstado() == EstadoEgreso.ANULADO) {
            throw new BusinessException("Un egreso anulado no puede procesarse");
        }
        entity.setEstado(EstadoEgreso.PROCESADO);
        entity.setFechaActualizacion(LocalDateTime.now());
        return IEgresoService.toResponse(egresoRepository.save(entity));
    }

    @Override
    @Transactional
    public EgresoResponse annul(Long id) {
        EgresoEntity entity = find(id);
        if (entity.getEstado() == EstadoEgreso.ANULADO) {
            throw new BusinessException("El egreso ya está anulado");
        }
        entity.setEstado(EstadoEgreso.ANULADO);
        entity.setFechaActualizacion(LocalDateTime.now());
        return IEgresoService.toResponse(egresoRepository.save(entity));
    }

    /**
     * Importación CSV (ESP-04 §4.5, ESP-08 §7.3). Se conserva el parser del
     * monolito: separador {@code ;}, se salta la cabecera, mínimo ocho columnas
     * y la novena ({@code documentoAsociado}) opcional. Todo error incluye el
     * número de fila.
     */
    @Override
    @Transactional
    public List<EgresoResponse> importCsv(MultipartFile file, UsuarioPrincipal usuario) {
        if (file == null || file.isEmpty()) throw new BusinessException("Seleccione un archivo CSV");
        validarUsuario(usuario);
        List<EgresoResponse> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank() || (row == 1 && line.toLowerCase().contains("tipodocumento"))) continue;
                String[] values = line.split(";", -1);
                if (values.length < 8) {
                    throw new BusinessException(
                            "Fila " + row + ": se requieren 8 columnas separadas por punto y coma");
                }
                EgresoRequest request = new EgresoRequest();
                request.setTipoDocumento(values[0].trim());
                request.setNumeroDocumento(values[1].trim());
                request.setProveedor(values[2].trim());
                request.setFecha(LocalDate.parse(values[3].trim()));
                request.setSubtotal(new BigDecimal(values[4].trim()));
                request.setImpuesto(new BigDecimal(values[5].trim()));
                request.setTotal(new BigDecimal(values[6].trim()));
                request.setMotivo(values[7].trim());
                request.setDocumentoAsociado(values.length > 8 ? values[8].trim() : null);
                validarImportado(request, row);
                imported.add(IEgresoService.toResponse(egresoRepository.save(map(request, usuario))));
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException("No se pudo leer el CSV: " + ex.getMessage());
        }
        if (imported.isEmpty()) throw new BusinessException("El archivo no contiene egresos válidos");
        return imported;
    }

    private EgresoEntity find(Long id) {
        return egresoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Egreso no encontrado"));
    }

    private EgresoEntity map(EgresoRequest request, UsuarioPrincipal usuario) {
        return EgresoEntity.builder()
                .comprobante(correlativoService.next(TIPO_CORRELATIVO, PREFIJO_CORRELATIVO))
                .tipoDocumento(request.getTipoDocumento().trim())
                .numeroDocumento(request.getNumeroDocumento().trim())
                .proveedor(request.getProveedor().trim())
                .fecha(request.getFecha())
                .subtotal(request.getSubtotal())
                .impuesto(request.getImpuesto())
                .total(request.getTotal())
                .motivo(request.getMotivo().trim())
                .documentoAsociado(blankToNull(request.getDocumentoAsociado()))
                .estado(EstadoEgreso.REGISTRADO)
                .usuarioId(usuario.id())
                .usuarioUsuario(usuario.usuario())
                .usuarioNombres(usuario.nombres())
                .usuarioApellidos(usuario.apellidos())
                .usuarioRol(usuario.rol())
                .fechaRegistro(LocalDateTime.now())
                .build();
    }

    private void validarImportado(EgresoRequest request, int row) {
        if (request.getTipoDocumento().isBlank() || request.getNumeroDocumento().isBlank()
                || request.getProveedor().isBlank() || request.getMotivo().isBlank()
                || request.getSubtotal().signum() < 0 || request.getImpuesto().signum() < 0
                || request.getTotal().signum() <= 0) {
            throw new BusinessException("Fila " + row + ": datos incompletos o montos inválidos");
        }
    }

    /**
     * R-02: sin los cinco campos del token no se escribe el snapshot del
     * usuario (ESP-03 §4.1). La base también lo rechaza con columnas NOT NULL.
     */
    private void validarUsuario(UsuarioPrincipal usuario) {
        if (usuario == null || usuario.id() == null
                || isBlank(usuario.usuario()) || isBlank(usuario.nombres())
                || isBlank(usuario.apellidos()) || isBlank(usuario.rol())) {
            throw new BusinessException(
                    "El token no contiene los datos del usuario necesarios para registrar el egreso");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
