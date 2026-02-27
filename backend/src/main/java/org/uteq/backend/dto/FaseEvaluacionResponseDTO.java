package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaseEvaluacionResponseDTO {

    private Long idFase;
    private String nombre;
    private String tipo;
    private Integer peso;
    private Integer orden;
    private Long idPlantilla;
    private String nombrePlantilla;
    private List<String> evaluadoresPermitidos;
    private Boolean estado;
}
