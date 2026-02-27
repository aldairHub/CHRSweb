package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardEvaluacionDTO {

    private Long postulantesActivos;
    private Long reunionesProgramadas;
    private Long evaluacionesCompletas;
    private Long pendientesHoy;
}
