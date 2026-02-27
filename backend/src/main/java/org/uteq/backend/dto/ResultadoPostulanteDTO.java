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
public class ResultadoPostulanteDTO {

    private Long idPostulante;
    private String nombreCompleto;
    private String materia;
    private List<ResultadoFaseDTO> fasesResultados;
    private Double calificacionTotal;
    private Integer progreso;
    /** aprobado_contratar | aprobado_espera | no_aprobado | segunda_ronda */
    private String decision;
    private String justificacionDecision;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResultadoFaseDTO {
        private Long idFase;
        private String nombreFase;
        private Integer peso;
        private Double calificacion;
        private Double ponderado;
        private List<String> evaluadores;
        /** completada | programada | bloqueada | pendiente */
        private String estado;
        private List<EvaluacionResponseDTO> evaluaciones;
    }
}
