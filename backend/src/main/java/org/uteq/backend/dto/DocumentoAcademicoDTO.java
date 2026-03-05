package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoAcademicoDTO {
    private Long idDocumento;
    private String descripcion;
    private String urlDocumento;
    private LocalDateTime fechaSubida;
}
