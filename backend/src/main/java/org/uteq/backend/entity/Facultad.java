package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "facultad", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_facultad_nombre_facultad", columnNames = "nombre_facultad")
})
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facultad")
    private Long idFacultad;

    @Column(name = "nombre_facultad", nullable = false)
    private String nombreFacultad;

    @Column(name = "estado", nullable = false)
    private boolean estado;

}