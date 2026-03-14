package org.uteq.backend.dto;

// ============================================================
// DTO — Info del postulante (SP 5)
// ============================================================

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostulanteInfoDTO {

    private Long idPostulante;
    private String nombres;
    private String apellidos;
    private String identificacion;
    private String correo;
    private Long idPostulacion;
    private String estadoPostulacion;
    private String nombreMateria;
    private String nombreCarrera;
    private String nombreArea;

    // Ventana de subida de documentos (independiente del estado de la convocatoria)
    private Boolean documentosAbiertos;     // true = puede subir hoy
    private String  fechaLimiteDocumentos;  // fecha límite (puede ser posterior al cierre)
}
