package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaEvaluacionResponseDTO {

    private Long idPlantilla;
    private String codigo;
    private String nombre;
    private Long idFase;
    private String nombreFase;
    private Integer numeroCriterios;
    private String ultimaModificacion; // formateado dd/MM/yyyy
    private Boolean estado;
}
