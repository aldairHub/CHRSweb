package org.uteq.backend.dto;

import lombok.Data;

@Data
public class QueryMonitorDTO {

    private String  queryTexto;
    private Long    llamadas;
    private Double  tiempoPromedioMs;
    private Double  tiempoTotalMs;
    private Double  filasPromedio;
    private Double  porcentajeTiempo;

}
