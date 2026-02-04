package org.uteq.backend.dto;

public class AreaConocimientoResponseDTO {

    private Long idArea;
    private String nombreArea;

    public AreaConocimientoResponseDTO(Long idArea, String nombreArea) {
        this.idArea = idArea;
        this.nombreArea = nombreArea;
    }

    public Long getIdArea() {
        return idArea;
    }

    public String getNombreArea() {
        return nombreArea;
    }
}
