package org.uteq.backend.dto;

import lombok.Data;

@Data
public class OpcionDTO {
    private Integer idOpcion;
    private String  nombre;
    private String  descripcion;
    private String  ruta;
    private Integer orden;
    private Boolean soloLectura;
}
