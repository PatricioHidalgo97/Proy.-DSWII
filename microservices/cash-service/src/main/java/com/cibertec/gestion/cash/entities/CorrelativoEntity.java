package com.cibertec.gestion.cash.entities;

import jakarta.persistence.*;
import lombok.*;

/** Correlativo por tipo de documento (ESP-12 R-05). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "correlativos")
public class CorrelativoEntity {

    @Id
    @Column(length = 20)
    private String tipo;

    @Column(name = "ultimo_numero", nullable = false)
    private long ultimoNumero;
}
