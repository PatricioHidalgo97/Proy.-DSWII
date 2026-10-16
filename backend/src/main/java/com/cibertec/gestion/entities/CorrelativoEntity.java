package com.cibertec.gestion.entities;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private long ultimoNumero;
}
