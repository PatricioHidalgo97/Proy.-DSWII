package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.EgresoRequest;
import com.cibertec.gestion.cash.entities.EgresoEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Tesorería: las cinco operaciones de egresos de ESP-04 §4.5 más la
 * importación CSV de ESP-08 §7.3.
 *
 * <p>{@link EgresoResponse} es la forma de ESP-04 §5.6 y vive aquí para no
 * añadir un archivo fuera de los entregables de PLAN-05.</p>
 */
public interface IEgresoService {

    List<EgresoResponse> getByMonth(YearMonth month);

    EgresoResponse getById(Long id);

    EgresoResponse create(EgresoRequest request, com.cibertec.gestion.cash.security.UsuarioPrincipal usuario);

    EgresoResponse process(Long id);

    EgresoResponse annul(Long id);

    List<EgresoResponse> importCsv(MultipartFile file, com.cibertec.gestion.cash.security.UsuarioPrincipal usuario);

    record EgresoResponse(
            Long id,
            String comprobante,
            String tipoDocumento,
            String numeroDocumento,
            String proveedor,
            LocalDate fecha,
            BigDecimal subtotal,
            BigDecimal impuesto,
            BigDecimal total,
            String motivo,
            String documentoAsociado,
            String estado,
            UsuarioAnidado usuario,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaActualizacion
    ) {
    }

    record UsuarioAnidado(
            Long id,
            String usuario,
            String nombres,
            String apellidos,
            String rol
    ) {
    }

    static EgresoResponse toResponse(EgresoEntity entity) {
        if (entity == null) return null;
        return new EgresoResponse(
                entity.getId(),
                entity.getComprobante(),
                entity.getTipoDocumento(),
                entity.getNumeroDocumento(),
                entity.getProveedor(),
                entity.getFecha(),
                entity.getSubtotal(),
                entity.getImpuesto(),
                entity.getTotal(),
                entity.getMotivo(),
                entity.getDocumentoAsociado(),
                entity.getEstado() == null ? null : entity.getEstado().name(),
                new UsuarioAnidado(
                        entity.getUsuarioId(),
                        entity.getUsuarioUsuario(),
                        entity.getUsuarioNombres(),
                        entity.getUsuarioApellidos(),
                        entity.getUsuarioRol()),
                entity.getFechaRegistro(),
                entity.getFechaActualizacion());
    }
}
