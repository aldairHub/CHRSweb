package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

// ─── REQUEST ───────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaseEvaluacionRequestDTO {

    private String nombre;

    /** automatica | reunion | practica | decision */
    private String tipo;

    private Integer peso;

    private Integer orden;

    /** Lista de evaluadores permitidos */
    private List<String> evaluadoresPermitidos;

    private Boolean estado;
}
