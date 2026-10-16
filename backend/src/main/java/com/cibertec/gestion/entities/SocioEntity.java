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
@Table(name = "socios", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
public class SocioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 30)
    private String accion;

    @Column(nullable = false)
    private Integer etapa;

    private LocalDate fechaNacimiento;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
