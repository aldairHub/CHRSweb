package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_accion")
@Data
@NoArgsConstructor
public class HistorialAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    // FK → proceso_evaluacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proceso", nullable = false)
    private ProcesoEvaluacion proceso;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "usuario")
    private String usuario;
}
