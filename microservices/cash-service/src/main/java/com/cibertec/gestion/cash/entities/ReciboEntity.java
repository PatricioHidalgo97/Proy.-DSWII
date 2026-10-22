package com.cibertec.gestion.cash.entities;

import com.cibertec.gestion.cash.enums.TipoRecibo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Recibo de caja (ESP-04 §5.5) sobre {@code gestion_cash.recibos}.
 *
 * <p>El vínculo con la cuenta es local y único ({@code uk_recibos_cuenta}).
 * Banco y usuario son snapshots con identificador remoto: no hay clave foránea
 * hacia catálogo ni hacia identidad (ESP-03 §3, §4.1, §4.5).</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recibos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recibos_correlativo", columnNames = "correlativo"),
                @UniqueConstraint(name = "uk_recibos_cuenta", columnNames = "cuenta_cobrar_id")
        })
public class ReciboEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String correlativo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecibo tipo;

    @OneToOne
    @JoinColumn(name = "cuenta_cobrar_id", unique = true)
    private CuentaCobrarEntity cuentaCobrar;

    // --- snapshot del banco (ESP-03 §4.5) ---
    @Column(name = "banco_id")
    private Long bancoId;

    @Column(name = "banco_nombre", length = 100)
    private String bancoNombre;

    @Column(name = "banco_numero_cuenta", length = 40)
    private String bancoNumeroCuenta;

    @Column(name = "banco_cci", length = 30)
    private String bancoCci;

    @Column(name = "banco_moneda", length = 3)
    private String bancoMoneda;

    @Column(length = 150)
    private String depositante;

    @Column(length = 80)
    private String categoria;

    @Column(length = 250)
    private String concepto;

    @Column(name = "fecha_deposito")
    private LocalDate fechaDeposito;

    // --- snapshot del usuario que emite (ESP-03 §4.1) ---
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
}
