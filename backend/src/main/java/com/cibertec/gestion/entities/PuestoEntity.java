package com.cibertec.gestion.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "puestos", uniqueConstraints = @UniqueConstraint(columnNames = "numero"))
public class PuestoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(length = 150)
    private String nombreInquilino;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @ManyToOne
    @JoinColumn(name = "socio_id")
    private SocioEntity socio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "giro_id", nullable = false)
    private GiroEntity giro;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
