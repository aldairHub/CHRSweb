package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "convocatoria", schema = "public")
@Data
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convocatoria")
    private Long idConvocatoria;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDate fechaPublicacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    // Valores válidos según CHECK de BD: 'abierta' | 'cerrada' | 'cancelada'
    @Column(name = "estado_convocatoria", nullable = false)
    private String estadoConvocatoria;

    public Convocatoria() {
        this.estadoConvocatoria = "abierta";
        this.fechaPublicacion   = LocalDate.now();
    }
}