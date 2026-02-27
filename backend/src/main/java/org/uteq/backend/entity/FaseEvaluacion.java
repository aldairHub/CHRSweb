package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fase_evaluacion")
@Data
@NoArgsConstructor
public class FaseEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fase")
    private Long idFase;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    // valores: automatica | reunion | practica | decision
    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "peso", nullable = false)
    private Integer peso;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    // Evaluadores permitidos almacenados como texto separado por comas
    @Column(name = "evaluadores_permitidos", columnDefinition = "TEXT")
    private String evaluadoresPermitidos;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    // Relación con plantilla (una fase puede tener una plantilla activa)
    @OneToOne(mappedBy = "fase", fetch = FetchType.LAZY)
    private PlantillaEvaluacion plantilla;
}
