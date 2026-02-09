package org.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;


public class FacultadRequestDTO {

    @NotBlank
    @JsonAlias({ "nombre_facultad", "nombreFacultad", "nombre" })
    private String nombreFacultad;

    private Boolean estado; // Boolean para poder defaultear si no viene

    public String getNombreFacultad() {
        return nombreFacultad;
    }

    public void setNombreFacultad(String nombreFacultad) {
        this.nombreFacultad = nombreFacultad;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }
}