package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

// ─── PostulanteDTO (vista lista) ───────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostulanteEvaluacionDTO {

    private Long idPostulante;   // = idProceso en backend
    private String codigo;
    private String nombres;
    private String apellidos;
    private String cedula;
    private String materia;
    private String faseActual;
    private Integer progreso;
    /** en_proceso | completado | rechazado | pendiente */
    private String estadoGeneral;
}
