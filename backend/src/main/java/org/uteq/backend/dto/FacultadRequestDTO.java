package org.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FacultadRequestDTO {

    @JsonProperty("nombre")
    private String nombreFacultad;
    private boolean estado;

    public String getNombreFacultad() {
        return nombreFacultad;
    }

    public void setNombreFacultad(String nombreFacultad) {
        this.nombreFacultad = nombreFacultad;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
