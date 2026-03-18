package org.uteq.backend.dto;

import lombok.Data;

@Data
public class RequisitoPrepostulacionRequestDTO {
    private String  nombre;
    private String  descripcion;
    private Integer orden = 0;
}