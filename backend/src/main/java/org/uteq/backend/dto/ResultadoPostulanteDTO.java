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
    private Double calificacionTotal;  // puntaje entrevistas sobre 100
    private Integer progreso;
    private String decision;
    private String justificacionDecision;

    // ── Puntajes ponderados ──────────────────────────────
    private Double puntajeMatriz;      // sobre 104
    private Double puntajeEntrevista;  // sobre 100
    private Double puntajeFinal;       // sobre 100 (ponderado 50/50)

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
        private String estado;
        private List<EvaluacionResponseDTO> evaluaciones;
    }
}