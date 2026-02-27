package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
@Data
@Entity
@Table(name = "postulante")
public class Postulante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_postulante")
    private Long idPostulante;

    @Column(name = "nombres_postulante", nullable = false)
    private String nombresPostulante;

    @Column(name = "apellidos_postulante", nullable = false)
    private String apellidosPostulante;

    @Column(name = "identificacion", nullable = false, unique = true)
    private String identificacion;

    @Column(name = "correo_postulante", nullable = false)
    private String correoPostulante;

    @Column(name = "telefono_postulante", nullable = false)
    private String telefonoPostulante;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // FK → usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // FK → prepostulacion
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prepostulacion", unique = true)
    private Prepostulacion prepostulacion;

}