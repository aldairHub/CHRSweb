package org.uteq.backend.dto;

import lombok.Data;

import java.util.List;
@Data
public class MonitorResumenDTO {

    private boolean extensionDisponible;
    private long    totalQueriesUnicas;
    private String  queryMasLenta;
    private double  tiempoMasLento;
    private String  queryMasFrecuente;
    private long    llamadasMasFrecuente;
    private List<QueryMonitorDTO> queries;

}
