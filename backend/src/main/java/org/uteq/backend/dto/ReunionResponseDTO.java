package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReunionResponseDTO {

    private Long idReunion;
    private Long idPostulante;
    private Long idFase;
    private String nombrePostulante;
    private String nombreFase;
    private String fecha;
    private String hora;
    private Integer duracion;
    private String modalidad;
    private String enlace;
    private List<String> evaluadores;
    private String observaciones;
    /** programada | en_curso | completada | cancelada */
    private String estado;
}
