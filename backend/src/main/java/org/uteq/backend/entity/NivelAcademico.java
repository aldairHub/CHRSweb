package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "nivel_academico",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_nivel_nombre", columnNames = "nombre")
        })
public class NivelAcademico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel")
    private Long idNivel;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "estado", nullable = false)
    private boolean estado = true;
}