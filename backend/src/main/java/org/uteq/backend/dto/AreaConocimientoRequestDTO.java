package org.uteq.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AreaConocimientoRequestDTO {

    @NotBlank
    private String nombreArea;

    public String getNombreArea() {
        return nombreArea;
    }

    public void setNombreArea(String nombreArea) {
        this.nombreArea = nombreArea;
    }
}
