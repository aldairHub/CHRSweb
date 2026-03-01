package org.uteq.backend.dto;

import lombok.Data;

@Data
public class AsignarOpcionDTO {
    private Integer idRolApp;
    private Integer idOpcion;
    private Boolean soloLectura = false;
}
