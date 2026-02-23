package org.uteq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

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

    // FK → usuario (ya tienes la entity Usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // FK → prepostulacion (ya tienes la entity Prepostulacion)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prepostulacion", unique = true)
    private Prepostulacion prepostulacion;

    // Getters y Setters
    public Long getIdPostulante() { return idPostulante; }
    public void setIdPostulante(Long v) { this.idPostulante = v; }

    public String getNombresPostulante() { return nombresPostulante; }
    public void setNombresPostulante(String v) { this.nombresPostulante = v; }

    public String getApellidosPostulante() { return apellidosPostulante; }
    public void setApellidosPostulante(String v) { this.apellidosPostulante = v; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }

    public String getCorreoPostulante() { return correoPostulante; }
    public void setCorreoPostulante(String v) { this.correoPostulante = v; }

    public String getTelefonoPostulante() { return telefonoPostulante; }
    public void setTelefonoPostulante(String v) { this.telefonoPostulante = v; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate v) { this.fechaNacimiento = v; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario v) { this.usuario = v; }

    public Prepostulacion getPrepostulacion() { return prepostulacion; }
    public void setPrepostulacion(Prepostulacion v) { this.prepostulacion = v; }
}