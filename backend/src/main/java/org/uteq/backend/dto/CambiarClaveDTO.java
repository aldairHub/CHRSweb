package org.uteq.backend.dto;

import lombok.Data;

@Data
public class CambiarClaveDTO {
    private String claveActual;
    private String claveNueva;
    private String claveNuevaConfirmacion;
}