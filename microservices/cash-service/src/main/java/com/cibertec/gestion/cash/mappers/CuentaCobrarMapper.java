package com.cibertec.gestion.cash.mappers;

import com.cibertec.gestion.cash.entities.CuentaCobrarEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección de cuenta por cobrar (ESP-04 §5.4). Los anidamientos se construyen
 * exclusivamente desde los snapshots: nunca se consulta catálogo al leer.
 *
 * <p><b>Desviación conocida (CA-02 de PLAN-04).</b> El esquema de
 * {@code gestion_cash} guarda del puesto {@code puesto_numero},
 * {@code puesto_nombre_inquilino}, {@code puesto_giro_nombre},
 * {@code puesto_socio_codigo}, {@code puesto_socio_nombres} y
 * {@code puesto_socio_apellidos}, pero no los identificadores del socio ni del
 * giro del puesto: ESP-03 §4.3 no los incluye y la migración
 * {@code V1__creacion_tesoreria.sql} es inmutable. Por eso
 * {@code puesto.socio.id} y {@code puesto.giro.id} se serializan como
 * {@code null}.</p>
 *
 * <p>No se resuelven al leer: consultar catálogo en un listado rompería
 * ESP-03 §5 y §8 (el histórico debe verse con catálogo caído) y deducirlos del
 * nombre o del código sería adivinar. Los nombres de campo se conservan y los
 * {@code null} no se omiten (ESP-04 §7.5), así que la forma de ESP-04 §5.4 se
 * mantiene intacta salvo esos dos valores. Cerrar la brecha exige una migración
 * nueva con {@code puesto_socio_id} y {@code puesto_giro_id} y rellenarlas en
 * el alta; queda fuera de este plan porque prohíbe tocar
 * {@code database/flyway/cash/**}.</p>
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
