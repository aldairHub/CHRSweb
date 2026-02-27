package org.uteq.backend.dto;

import lombok.Data;

@Data
public class TipoDocumentoDTO {

    private Long idTipoDocumento;
    private String nombre;
    private String descripcion;
    private Boolean obligatorio;
    private Boolean activo;

}