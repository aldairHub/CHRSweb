package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

// ─── REQUEST (mapea EvaluacionCreatePayload del frontend) ─────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionRequestDTO {

    private Long idReunion;

    private List<DetalleRequest> criterios;

    private String observaciones;

    private Boolean declaroSinConflicto;

    private String firmaDigital;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleRequest {
        private Long idCriterio;
        private Double nota;
        private String observacion;
    }
}
