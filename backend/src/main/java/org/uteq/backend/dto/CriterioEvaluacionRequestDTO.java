package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// ─── REQUEST ───────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriterioEvaluacionRequestDTO {

    private String nombre;
    private String descripcion;
    private Integer peso;
    /** 1-5 | 1-10 | 0-100 */
    private String escala;
    private String rubrica;
    private Long idPlantilla;
}
