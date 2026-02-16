package org.uteq.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "materia", schema = "public")
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_materia")
    private Long idMateria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Column(name = "nombre_materia", nullable = false)
    private String nombreMateria;

    @Column(name = "nivel", nullable = false)
    private Long nivel;

    public Materia() {}

    public Long getIdMateria() { return idMateria; }
    public Carrera getCarrera() { return carrera; }
    public void setCarrera(Carrera carrera) { this.carrera = carrera; }

    public String getNombreMateria() { return nombreMateria; }
    public void setNombreMateria(String nombreMateria) { this.nombreMateria = nombreMateria; }

    public Long getNivel() { return nivel; }
    public void setNivel(Long nivel) { this.nivel = nivel; }
}