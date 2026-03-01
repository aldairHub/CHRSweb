package org.uteq.backend.dto;

import lombok.Data;

@Data
public class SolicitudDocenteDTO {
    private Long   idSolicitud;
    private Long   idMateria;
    private String nombreMateria;
    private Long   idCarrera;
    private String nombreCarrera;
    private Long   idArea;
    private String nombreArea;
    private String justificacion;
    private Long   cantidadDocentes;
    private String nivelAcademico;
    private String estadoSolicitud;
}