package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoReutilizableDTO {
    private Long   idDocumento;   // null para cédula/foto (no son PrepostulacionDocumento)
    private String tipo;          // "CEDULA" | "FOTO" | "ACADEMICO"
    private String descripcion;
    private String urlDocumento;
    private String fechaSubida;
}