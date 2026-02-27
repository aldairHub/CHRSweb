package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el proceso de evaluación de un postulante aprobado en prepostulación.
 * Vincula: Postulante (usuario) → SolicitudDocente → conjunto de FasesProceso
 */
@Entity
@Table(name = "proceso_evaluacion")
@Data
@NoArgsConstructor
public class ProcesoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proceso")
    private Long idProceso;

    // FK → postulante (ya existe en tu proyecto)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulante", nullable = false)
    private Postulante postulante;

    // FK → solicitud_docente (la vacante a la que aplica)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitud", nullable = false)
    private SolicitudDocente solicitudDocente;

    // Código generado: #P001, #P002, etc.
    @Column(name = "codigo", unique = true)
    private String codigo;

    // valores: en_proceso | completado | rechazado | pendiente
    @Column(name = "estado_general", nullable = false)
    private String estadoGeneral = "pendiente";

    // Nombre de la fase donde está actualmente
    @Column(name = "fase_actual")
    private String faseActual;

    // Porcentaje de progreso 0-100
    @Column(name = "progreso")
    private Integer progreso = 0;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio = LocalDateTime.now();

    // Decisión final del comité
    // valores: aprobado_contratar | aprobado_espera | no_aprobado | segunda_ronda
    @Column(name = "decision")
    private String decision;

    @Column(name = "justificacion_decision", columnDefinition = "TEXT")
    private String justificacionDecision;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @OneToMany(mappedBy = "proceso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FaseProceso> fasesProceso = new ArrayList<>();

    @OneToMany(mappedBy = "proceso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HistorialAccion> historial = new ArrayList<>();
}
