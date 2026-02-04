package org.uteq.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prepostulacion")
public class Prepostulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prepostulacion")
    private Long idPrepostulacion;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "identificacion", nullable = false, unique = true)
    private String identificacion;

    @Column(name = "correo", nullable = false)
    private String correo;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "estado_revision", nullable = false)
    private String estadoRevision = "pendiente"; // pendiente, aprobada, rechazada, observada

    @Column(name = "id_revisor")
    private Long idRevisor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "observaciones_revision")
    private String observacionesRevision;
}