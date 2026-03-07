package org.uteq.backend.dto;

import lombok.Data;

@Data
public class NivelAcademicoResponseDTO {
    private Long    idNivel;
    private String  nombre;
    private Integer orden;
    private boolean estado;
}