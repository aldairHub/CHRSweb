package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// ─── REQUEST ───────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEvaluacionRequestDTO {

    private String codigo;
    private String nombre;
    private Long idFase;
    private Boolean estado;
}
