package com.cibertec.gestion.cash.mappers;

import com.cibertec.gestion.cash.entities.ReciboEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección de recibo (ESP-04 §5.5). {@code cuentaCobrar}, {@code banco} y
 * {@code usuario} se construyen desde snapshots, sin llamadas remotas.
 */
public final class ReciboMapper {

    private ReciboMapper() {
    }

    public static ReciboResponse toResponse(ReciboEntity entity) {
        if (entity == null) return null;
        return new ReciboResponse(
                entity.getId(),
                entity.getCorrelativo(),
                entity.getFecha(),
                entity.getMonto(),
                entity.getTipo() == null ? null : entity.getTipo().name(),
                CuentaCobrarMapper.toReciboSummary(entity.getCuentaCobrar()),
                entity.getBancoId() == null ? null : new BancoAnidado(
                        entity.getBancoId(),
                        entity.getBancoNombre(),
                        entity.getBancoNumeroCuenta(),
                        entity.getBancoCci(),
                        entity.getBancoMoneda(),
                        true),
                entity.getDepositante(),
                entity.getCategoria(),
                entity.getConcepto(),
                entity.getFechaDeposito(),
                new UsuarioAnidado(
                        entity.getUsuarioId(),
                        entity.getUsuarioUsuario(),
                        entity.getUsuarioNombres(),
                        entity.getUsuarioApellidos(),
                        entity.getUsuarioRol())
        );
    }

    public record ReciboResponse(
            Long id,
            String correlativo,
            LocalDateTime fecha,
            BigDecimal monto,
            String tipo,
            CuentaCobrarMapper.CuentaEnRecibo cuentaCobrar,
            BancoAnidado banco,
            String depositante,
            String categoria,
            String concepto,
            LocalDate fechaDeposito,
            UsuarioAnidado usuario
    ) {
    }

    public record BancoAnidado(
            Long id,
            String nombre,
            String numeroCuenta,
            String cci,
            String moneda,
            boolean activo
    ) {
    }

    public record UsuarioAnidado(
            Long id,
            String usuario,
            String nombres,
            String apellidos,
            String rol
    ) {
    }
}
