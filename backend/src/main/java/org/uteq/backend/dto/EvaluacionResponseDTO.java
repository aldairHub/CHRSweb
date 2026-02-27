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
public class EvaluacionResponseDTO {

    private Long idEvaluacion;
    private Long idReunion;
    private Long idEvaluador;
    private String nombreEvaluador;
    private List<CriterioEvaluadoDTO> criterios;
    private String observaciones;
    private Double calificacionFinal;
    private String fechaEvaluacion;
    private String firmaDigital;
    private Boolean confirmada;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterioEvaluadoDTO {
        private Long idCriterio;
        private String nombre;
        private Integer peso;
        private Double nota;
        private String observacion;
    }
}
