package org.uteq.backend.dto;

import lombok.Data;

@Data
public class CrearOpcionDTO {
    private String  nombreModulo;
    private String  nombre;
    private String  descripcion;
    private String  ruta;
    private Integer orden = 0;
}
