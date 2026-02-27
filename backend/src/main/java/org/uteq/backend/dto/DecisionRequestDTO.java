package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// ─── Decisión final del comité ─────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequestDTO {

    /** aprobado_contratar | aprobado_espera | no_aprobado | segunda_ronda */
    private String decision;

    private String justificacion;
}
