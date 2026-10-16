package com.cibertec.gestion.entities;

import com.cibertec.gestion.enums.EstadoCuenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cuentas_cobrar")
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

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "puesto_id")
    private PuestoEntity puesto;

    @ManyToOne
    @JoinColumn(name = "socio_id")
    private SocioEntity socio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioEntity servicio;

    @Column(precision = 12, scale = 2)
    private BigDecimal lecturaInicial;

    @Column(precision = 12, scale = 2)
    private BigDecimal lecturaFinal;

    @Column(precision = 12, scale = 4)
    private BigDecimal costoUnitario;
}
