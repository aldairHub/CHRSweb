package org.uteq.backend.dto;

import lombok.Data;

@Data
public class DocumentoResponseDTO {

    private Long    idTipoDocumento;
    private String  nombreTipo;
    private Boolean obligatorio;
    private Long    idDocumento;
    private String  estadoValidacion;   // pendiente | subido | validado | rechazado | null
    private String  descripcionTipo;    // ← campo que faltaba
    private String  rutaArchivo;
    private String  fechaCarga;
    private String  observacionesIa;

}


