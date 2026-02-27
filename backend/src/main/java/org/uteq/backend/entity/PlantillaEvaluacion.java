package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plantilla_evaluacion")
@Data
@NoArgsConstructor
public class PlantillaEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long idPlantilla;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    // FK → fase_evaluacion
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fase", nullable = false)
    private FaseEvaluacion fase;

    @Column(name = "ultima_modificacion")
    private LocalDateTime ultimaModificacion = LocalDateTime.now();

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CriterioEvaluacion> criterios = new ArrayList<>();
}
