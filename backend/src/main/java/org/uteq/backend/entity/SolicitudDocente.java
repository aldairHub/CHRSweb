package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_docente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @ManyToOne
    @JoinColumn(name = "id_autoridad", nullable = false)
    private AutoridadAcademica autoridad;

    @ManyToOne
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne
    @JoinColumn(name = "id_materia", nullable = false)
    private Materia materia;

    @ManyToOne
    @JoinColumn(name = "id_area", nullable = false)
    private AreaConocimiento area;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "estado_solicitud", nullable = false)
    private String estadoSolicitud = "pendiente";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "cantidad_docentes", nullable = false)
    private Long cantidadDocentes;

    // Campos del perfil docente integrados
    @Column(name = "nivel_academico", nullable = false)
    private String nivelAcademico;

    @Column(name = "experiencia_profesional_min", nullable = false)
    private Long experienciaProfesionalMin;

    @Column(name = "experiencia_docente_min", nullable = false)
    private Long experienciaDocenteMin;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}