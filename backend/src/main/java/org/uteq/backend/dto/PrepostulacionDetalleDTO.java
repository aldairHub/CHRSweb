package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrepostulacionDetalleDTO {
    private Long id;
    private String identificacion;
    private String nombreCompleto;
    private String correo;
    private String estado;
    private LocalDateTime fechaEnvio;
    private Boolean tieneDocumentos;  // true si tiene los 3 documentos
}