package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad Prepostulacion
 */
@Entity
@Table(name = "prepostulacion")
@Data
public class Prepostulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prepostulacion")
    private Long idPrepostulacion;

    @Column(nullable = false)
    private String apellidos;

    @Column(nullable = false)
    private String correo;

    @Column(name = "estado_revision")
    private String estadoRevision;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "id_revisor")
    private Long idRevisor;

    @Column(nullable = false)
    private String identificacion;

    @Column(nullable = false)
    private String nombres;

    @Column(name = "observaciones_revision", length = 1000)
    private String observacionesRevision;

    @Column(name = "url_cedula", length = 500)
    private String urlCedula;

    @Column(name = "url_foto", length = 500)
    private String urlFoto;

    @Column(name = "url_prerrequisitos", length = 500)
    private String urlPrerrequisitos;

    // Constructor vacío requerido por JPA
    public Prepostulacion() {
        this.fechaEnvio = LocalDateTime.now();
        this.estadoRevision = "PENDIENTE";
    }
}