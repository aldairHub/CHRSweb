package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evaluacion")
@Data
@NoArgsConstructor
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    // FK → reunion_evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reunion", nullable = false)
    private Reunion reunion;

    // FK → usuario (el evaluador que llenó este formulario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluador", nullable = false)
    private Usuario evaluador;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "calificacion_final")
    private Double calificacionFinal;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion = LocalDateTime.now();

    // Firma digital guardada como base64
    @Column(name = "firma_digital", columnDefinition = "TEXT")
    private String firmaDigital;

    @Column(name = "declaro_sin_conflicto", nullable = false)
    private Boolean declaroSinConflicto = false;

    @Column(name = "confirmada", nullable = false)
    private Boolean confirmada = false;

    @OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleEvaluacion> detalles = new ArrayList<>();
}
