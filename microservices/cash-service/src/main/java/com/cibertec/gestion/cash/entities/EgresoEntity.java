package com.cibertec.gestion.cash.entities;

import com.cibertec.gestion.cash.enums.EstadoEgreso;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Egreso de caja (ESP-04 §5.6) sobre {@code gestion_cash.egresos}.
 *
 * <p>El usuario que registra el egreso es un snapshot con identificador remoto:
 * no hay relación JPA ni clave foránea hacia identidad (ESP-03 §3, §4.1). Los
 * campos son los de la tabla de {@code V1__creacion_tesoreria.sql}; el snapshot
 * ya lo trae el seed de {@code V4}.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "egresos",
        uniqueConstraints = @UniqueConstraint(name = "uk_egresos_comprobante", columnNames = "comprobante"))
public class EgresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String comprobante;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(nullable = false, length = 150)
    private String proveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal impuesto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(name = "documento_asociado", length = 80)
    private String documentoAsociado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEgreso estado;

    // --- snapshot del usuario que registra (ESP-03 §4.1) ---
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "usuario_usuario", nullable = false, length = 50)
    private String usuarioUsuario;

    @Column(name = "usuario_nombres", nullable = false, length = 100)
    private String usuarioNombres;

    @Column(name = "usuario_apellidos", nullable = false, length = 100)
    private String usuarioApellidos;

    @Column(name = "usuario_rol", nullable = false, length = 20)
    private String usuarioRol;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
