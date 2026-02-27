package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "criterio_evaluacion")
@Data
@NoArgsConstructor
public class CriterioEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long idCriterio;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "peso", nullable = false)
    private Integer peso;

    // valores: 1-5 | 1-10 | 0-100
    @Column(name = "escala", nullable = false)
    private String escala = "1-5";

    @Column(name = "rubrica", columnDefinition = "TEXT")
    private String rubrica;

    // FK → plantilla_evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla", nullable = false)
    private PlantillaEvaluacion plantilla;
}
