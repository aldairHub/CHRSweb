package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostulanteInfoDTO {

    private Long   idPostulante;
    private String nombres;
    private String apellidos;
    private String identificacion;
    private String correo;
    private Long   idPostulacion;
    private String estadoPostulacion;
    private String nombreMateria;
    private String nombreCarrera;
    private String nombreArea;

    // Ventana de subida de documentos
    private Boolean documentosAbiertos;
    private String  fechaLimiteDocumentos;

    private Long   idConvocatoria;
    private String nombreConvocatoria;   // título de la convocatoria
}