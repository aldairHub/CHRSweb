package org.uteq.backend.dto;

public class MateriaRequestDTO {

    private String nombre;
    private Long idCarrera;
    private Long nivel;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Long getIdCarrera() { return idCarrera; }
    public void setIdCarrera(Long idCarrera) { this.idCarrera = idCarrera; }

    public Long getNivel() { return nivel; }
    public void setNivel(Long nivel) { this.nivel = nivel; }
}
