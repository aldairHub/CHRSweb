package org.uteq.backend.dto;

import lombok.Data;

@Data
public class CarreraRequestDTO {

    private Long idFacultad;
    private String nombreCarrera;
    private String modalidad;
    private boolean estado;

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
