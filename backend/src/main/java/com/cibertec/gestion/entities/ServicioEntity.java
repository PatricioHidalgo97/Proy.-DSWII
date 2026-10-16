package com.cibertec.gestion.entities;

import com.cibertec.gestion.enums.CargoA;
import com.cibertec.gestion.enums.Moneda;
import com.cibertec.gestion.enums.Recurrencia;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "servicios")
public class ServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Recurrencia recurrencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CargoA cargoA;

    @Column(nullable = false)
    private boolean dependeConsumo;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
