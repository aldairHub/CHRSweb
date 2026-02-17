package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDocenteResponseDTO {

    private Long idSolicitud;
    private String usuarioApp;
    private Long idAutoridad;
    private String nombreAutoridad;

    private Long idCarrera;
    private String nombreCarrera;
    private String modalidadCarrera;
    private Long idFacultad;
    private String nombreFacultad;

    private Long idMateria;
    private String nombreMateria;
    private Long nivelMateria;

    private Long idArea;
    private String nombreArea;

    private LocalDateTime fechaSolicitud;
    private String estadoSolicitud;
    private String justificacion;
    private Long cantidadDocentes;

    private String nivelAcademico;
    private Long experienciaProfesionalMin;
    private Long experienciaDocenteMin;
    private String observaciones;
}
