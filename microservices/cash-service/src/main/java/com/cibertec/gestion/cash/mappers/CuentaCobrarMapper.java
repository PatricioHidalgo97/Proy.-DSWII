package com.cibertec.gestion.cash.mappers;

import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección de cuenta por cobrar (ESP-04 §5.4). Los anidamientos se construyen
 * exclusivamente desde los snapshots: nunca se consulta catálogo al leer.
 *
 * <p>El esquema no guarda el id del socio ni el id del giro del puesto, así que
 * esos dos identificadores se serializan como {@code null} dentro de
 * {@code puesto.socio} y {@code puesto.giro}. Los nombres de campo se conservan
 * y los {@code null} no se omiten (ESP-04 §7.5).</p>
 */
public final class CuentaCobrarMapper {

    private CuentaCobrarMapper() {
    }

    public static CuentaCobrarResponse toResponse(CuentaCobrarEntity entity) {
        if (entity == null) return null;
        PuestoAnidado puesto = entity.getPuestoId() == null ? null : new PuestoAnidado(
                entity.getPuestoId(),
                entity.getPuestoNumero(),
                entity.getPuestoNombreInquilino(),
                entity.getPuestoSocioNombres() == null && entity.getPuestoSocioApellidos() == null
                        ? null
                        : new SocioAnidado(null,
                        entity.getPuestoSocioCodigo(),
                        entity.getPuestoSocioNombres(),
                        entity.getPuestoSocioApellidos()),
                new GiroAnidado(null, entity.getPuestoGiroNombre())
        );
        SocioAnidado socio = entity.getSocioId() == null ? null : new SocioAnidado(
                entity.getSocioId(),
                entity.getSocioCodigo(),
                entity.getSocioNombres(),
                entity.getSocioApellidos()
        );
        ServicioAnidado servicio = new ServicioAnidado(
                entity.getServicioId(),
                entity.getServicioNombre(),
                entity.getServicioMoneda(),
                entity.getServicioCargoA(),
                entity.isServicioDependeConsumo()
        );
        return new CuentaCobrarResponse(
                entity.getId(),
                entity.getPeriodo(),
                entity.getMonto(),
                entity.getEstado() == null ? null : entity.getEstado().name(),
                entity.getFechaCreacion(),
                puesto,
                socio,
                servicio,
                entity.getLecturaInicial(),
                entity.getLecturaFinal(),
                entity.getCostoUnitario()
        );
    }

    public static List<CuentaCobrarResponse> toResponse(List<CuentaCobrarEntity> entities) {
        return entities.stream().map(CuentaCobrarMapper::toResponse).toList();
    }

    /**
     * Forma reducida que se anida dentro de un recibo (ESP-04 §5.5).
     */
    public static CuentaEnRecibo toReciboSummary(CuentaCobrarEntity entity) {
        if (entity == null) return null;
        return new CuentaEnRecibo(
                entity.getId(),
                entity.getPeriodo(),
                entity.getMonto(),
                entity.getEstado() == null ? null : entity.getEstado().name(),
                new ServicioEnRecibo(entity.getServicioId(), entity.getServicioNombre()),
                entity.getSocioId() == null ? null : new SocioAnidado(
                        entity.getSocioId(),
                        entity.getSocioCodigo(),
                        entity.getSocioNombres(),
                        entity.getSocioApellidos()),
                entity.getPuestoId() == null ? null
                        : new PuestoEnRecibo(entity.getPuestoId(), entity.getPuestoNumero())
        );
    }

    public record CuentaCobrarResponse(
            Long id,
            String periodo,
            BigDecimal monto,
            String estado,
            LocalDateTime fechaCreacion,
            PuestoAnidado puesto,
            SocioAnidado socio,
            ServicioAnidado servicio,
            BigDecimal lecturaInicial,
            BigDecimal lecturaFinal,
            BigDecimal costoUnitario
    ) {
    }

    public record PuestoAnidado(
            Long id,
            String numero,
            String nombreInquilino,
            SocioAnidado socio,
            GiroAnidado giro
    ) {
    }

    public record SocioAnidado(
            Long id,
            String codigo,
            String nombres,
            String apellidos
    ) {
    }

    public record GiroAnidado(
            Long id,
            String nombre
    ) {
    }

    public record ServicioAnidado(
            Long id,
            String nombre,
            String moneda,
            String cargoA,
            boolean dependeConsumo
    ) {
    }

    public record CuentaEnRecibo(
            Long id,
            String periodo,
            BigDecimal monto,
            String estado,
            ServicioEnRecibo servicio,
            SocioAnidado socio,
            PuestoEnRecibo puesto
    ) {
    }

    public record ServicioEnRecibo(
            Long id,
            String nombre
    ) {
    }

    public record PuestoEnRecibo(
            Long id,
            String numero
    ) {
    }
}
