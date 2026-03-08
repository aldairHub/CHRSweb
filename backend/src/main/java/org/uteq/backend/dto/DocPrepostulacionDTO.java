package org.uteq.backend.dto;

import lombok.Data;

@Data
public class DocPrepostulacionDTO {
    private Long   idDocumento;
    private String descripcion;
    private String urlDocumento;
    private String fechaSubida;
}