package com.cibertec.gestion.entities;

import com.cibertec.gestion.enums.EstadoEgreso;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "egresos", uniqueConstraints = @UniqueConstraint(columnNames = "comprobante"))
public class EgresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String comprobante;

    @Column(nullable = false, length = 20)
    private String tipoDocumento;

    @Column(nullable = false, length = 30)
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

    @Column(length = 80)
    private String documentoAsociado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEgreso estado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaActualizacion;
}
