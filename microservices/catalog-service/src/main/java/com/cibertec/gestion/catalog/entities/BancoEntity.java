package com.cibertec.gestion.catalog.entities;

import com.cibertec.gestion.catalog.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bancos", uniqueConstraints = {
        @UniqueConstraint(columnNames = "numero_cuenta"),
        @UniqueConstraint(columnNames = "cci")
})
public class BancoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "numero_cuenta", nullable = false, length = 40)
    private String numeroCuenta;

    @Column(length = 30)
    private String cci;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda moneda;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
