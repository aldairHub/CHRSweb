package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reunion_evaluacion")
@Data
@NoArgsConstructor
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reunion")
    private Long idReunion;

    // FK → fase_proceso (la fase específica de este postulante)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fase_proceso", nullable = false)
    private FaseProceso faseProceso;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    // Duración en minutos
    @Column(name = "duracion", nullable = false)
    private Integer duracion;

    // valores: zoom | meet | teams | presencial
    @Column(name = "modalidad", nullable = false)
    private String modalidad;

    @Column(name = "enlace")
    private String enlace;

    // Evaluadores asignados (ids separados por coma, se denormalizan para simplicidad)
    @Column(name = "evaluadores_ids", columnDefinition = "TEXT")
    private String evaluadoresIds;

    // Nombres de evaluadores para display rápido
    @Column(name = "evaluadores_nombres", columnDefinition = "TEXT")
    private String evaluadoresNombres;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // valores: programada | en_curso | completada | cancelada
    @Column(name = "estado", nullable = false)
    private String estado = "programada";
}
