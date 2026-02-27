package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Instancia de una FaseEvaluacion dentro de un ProcesoEvaluacion específico.
 * Registra el estado, calificación y fecha de cada fase para cada postulante.
 */
@Entity
@Table(name = "fase_proceso")
@Data
@NoArgsConstructor
public class FaseProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fase_proceso")
    private Long idFaseProceso;

    // FK → proceso_evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proceso", nullable = false)
    private ProcesoEvaluacion proceso;

    // FK → fase_evaluacion (la definición de la fase)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fase", nullable = false)
    private FaseEvaluacion fase;

    // valores: completada | en_curso | pendiente | bloqueada
    @Column(name = "estado", nullable = false)
    private String estado = "bloqueada";

    @Column(name = "calificacion")
    private Double calificacion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    // Evaluadores asignados a esta fase-proceso (texto separado por comas)
    @Column(name = "evaluadores_asignados", columnDefinition = "TEXT")
    private String evaluadoresAsignados;
}
