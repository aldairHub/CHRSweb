package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "autoridad_academica")
public class AutoridadAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autoridad")
    private Long idAutoridad;

    @Column(name = "nombres", nullable = false, length = 255)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 255)
    private String apellidos;

    @Column(name = "correo", nullable = false, length = 255)
    private String correo;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @Column(name = "id_institucion", nullable = false)
    private Long idInstitucion;

    // 1 autoridad pertenece a 1 usuario
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

}