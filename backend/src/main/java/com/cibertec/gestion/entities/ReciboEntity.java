package com.cibertec.gestion.entities;

import com.cibertec.gestion.enums.TipoRecibo;
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
@Table(name = "recibos", uniqueConstraints = @UniqueConstraint(columnNames = "correlativo"))
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

    @ManyToOne
    @JoinColumn(name = "banco_id")
    private BancoEntity banco;

    @Column(length = 150)
    private String depositante;

    @Column(length = 80)
    private String categoria;

    @Column(length = 250)
    private String concepto;

    private java.time.LocalDate fechaDeposito;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}
