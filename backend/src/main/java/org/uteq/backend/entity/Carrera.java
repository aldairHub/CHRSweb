package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "carrera", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_carrera_nombre_carrera", columnNames = "nombre_carrera")
})
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Long idCarrera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_facultad", nullable = false)
    private Facultad facultad;

    @Column(name = "nombre_carrera", nullable = false)
    private String nombreCarrera;

    @Column(name = "modalidad", nullable = false)
    private String modalidad;

    @Column(name = "estado", nullable = false)
    private boolean estado;

}