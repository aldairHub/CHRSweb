package org.uteq.backend.dto;

import lombok.Data;

@Data
public class FacultadResponseDTO {

    private Long idFacultad;
    private String nombreFacultad;
    private boolean estado;

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
