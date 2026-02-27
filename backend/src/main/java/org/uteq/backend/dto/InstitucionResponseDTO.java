package org.uteq.backend.dto;

import lombok.Data;

@Data
public class InstitucionResponseDTO {

    private Long idInstitucion;
    private String nombreInstitucion;
    private String direccion;
    private String telefono;
    private boolean estado;

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
}
