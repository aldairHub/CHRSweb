package org.uteq.backend.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "carrera", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_carrera_nombre_carrera", columnNames = "nombre_carrera")
})
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Long idCarrera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_facultad", nullable = false)
    private Facultad facultad;

    @Column(name = "nombre_carrera", nullable = false)
    private String nombreCarrera;

    @Column(name = "modalidad", nullable = false)
    private String modalidad;

    @Column(name = "estado", nullable = false)
    private boolean estado;

    public Carrera() {}

    public Long getIdCarrera() { return idCarrera; }
    public Facultad getFacultad() { return facultad; }
    public void setFacultad(Facultad facultad) { this.facultad = facultad; }

    public String getNombreCarrera() { return nombreCarrera; }
    public void setNombreCarrera(String nombreCarrera) { this.nombreCarrera = nombreCarrera; }

    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }
}