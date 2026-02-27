package org.uteq.backend.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Entity
@Table(
        name = "postulacion",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_postulante_solicitud",
                columnNames = {"id_postulante", "id_solicitud"}
        )
)
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulacion")
    private Long idPostulacion;

    // FK → postulante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    // FK → solicitud_docente (ya tienes la entity SolicitudDocente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private SolicitudDocente solicitudDocente;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    // valores: pendiente | en_revision | aprobada | rechazada
    @Column(name = "estado_postulacion", nullable = false)
    private String estadoPostulacion = "pendiente";

    @Column(name = "preseleccion")
    private Boolean preseleccion;

}