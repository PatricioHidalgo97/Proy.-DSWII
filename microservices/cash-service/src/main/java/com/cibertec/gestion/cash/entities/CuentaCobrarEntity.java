package com.cibertec.gestion.cash.entities;

import com.cibertec.gestion.cash.enums.EstadoCuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cuenta por cobrar (ESP-04 §5.4) sobre {@code gestion_cash.cuentas_cobrar}.
 *
 * <p>No hay relaciones JPA hacia catálogo: los identificadores remotos
 * (`puestoId`, `socioId`, `servicioId`) se guardan sin clave foránea y con una
 * copia de los datos que el documento necesita para volver a mostrarse
 * (ESP-03 §3, §4). El único vínculo local es {@code recibos.cuenta_cobrar_id}.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cuentas_cobrar",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cuentas_puesto", columnNames = {"periodo", "servicio_id", "puesto_id"}),
                @UniqueConstraint(name = "uk_cuentas_socio", columnNames = {"periodo", "servicio_id", "socio_id"})
        })
public class CuentaCobrarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String periodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // --- identificadores remotos de catálogo (sin FK, ESP-03 §3) ---
    @Column(name = "puesto_id")
    private Long puestoId;

    @Column(name = "socio_id")
    private Long socioId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "lectura_inicial", precision = 12, scale = 2)
    private BigDecimal lecturaInicial;

    @Column(name = "lectura_final", precision = 12, scale = 2)
    private BigDecimal lecturaFinal;

    @Column(name = "costo_unitario", precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    // --- snapshot del servicio (ESP-03 §4.4) ---
    @Column(name = "servicio_nombre", nullable = false, length = 100)
    private String servicioNombre;

    @Column(name = "servicio_moneda", nullable = false, length = 3)
    private String servicioMoneda;

    @Column(name = "servicio_cargo_a", nullable = false, length = 20)
    private String servicioCargoA;

    @Column(name = "servicio_depende_consumo", nullable = false)
    private boolean servicioDependeConsumo;

    // --- snapshot del puesto (ESP-03 §4.3) ---
    @Column(name = "puesto_numero", length = 20)
    private String puestoNumero;

    @Column(name = "puesto_nombre_inquilino", length = 150)
    private String puestoNombreInquilino;

    @Column(name = "puesto_giro_nombre", length = 100)
    private String puestoGiroNombre;

    @Column(name = "puesto_giro_id")
    private Long puestoGiroId;

    @Column(name = "puesto_socio_id")
    private Long puestoSocioId;

    @Column(name = "puesto_socio_codigo", length = 20)
    private String puestoSocioCodigo;

    @Column(name = "puesto_socio_nombres", length = 100)
    private String puestoSocioNombres;

    @Column(name = "puesto_socio_apellidos", length = 100)
    private String puestoSocioApellidos;

    // --- snapshot del socio destinatario (ESP-03 §4.2) ---
    @Column(name = "socio_codigo", length = 20)
    private String socioCodigo;

    @Column(name = "socio_nombres", length = 100)
    private String socioNombres;

    @Column(name = "socio_apellidos", length = 100)
    private String socioApellidos;
}
