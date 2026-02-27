package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriterioEvaluacionResponseDTO {

    private Long idCriterio;
    private String nombre;
    private String descripcion;
    private Integer peso;
    private String escala;
    private String rubrica;
    private Long idPlantilla;
}
