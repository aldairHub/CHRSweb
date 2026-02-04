package org.uteq.backend.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "area_conocimiento", schema = "public")
public class AreaConocimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Long idArea;

    @Column(name = "nombre_area", nullable = false)
    private String nombreArea;

    public AreaConocimiento() {}

    public Long getIdArea() { return idArea; }
    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(String nombreArea) { this.nombreArea = nombreArea; }
}