package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detalle_evaluacion")
@Data
@NoArgsConstructor
public class DetalleEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    // FK → evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private Evaluacion evaluacion;

    // FK → criterio_evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_criterio", nullable = false)
    private CriterioEvaluacion criterio;

    @Column(name = "nota", nullable = false)
    private Double nota;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}
