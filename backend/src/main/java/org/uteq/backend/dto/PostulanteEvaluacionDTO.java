package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostulanteEvaluacionDTO {

    private Long idProceso;   // ← era "idPostulante", corregido para coincidir con frontend
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