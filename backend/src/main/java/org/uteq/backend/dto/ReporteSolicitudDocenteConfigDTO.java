package org.uteq.backend.dto;
import lombok.Data;
@Data
public class ReporteSolicitudDocenteConfigDTO {
    private String  titulo;
    private String  subtitulo;
    private String  formato       = "PDF";
    private String  orientacion   = "VERTICAL";
    private String  desde;
    private String  hasta;
    private String  estado;
    private String  facultad;
    private boolean incluirPortada          = true;
    private boolean incluirKpis             = true;
    private boolean incluirDetalle          = true;
    private boolean incluirGraficoEstados   = true;
    private boolean incluirGraficoCarreras  = true;
    private boolean incluirGraficoAreas     = true;
    private boolean incluirGraficoTemporal  = true;
    private String  colorPrimario  = "#2563EB";
    private boolean mostrarNumeroPagina    = true;
    private boolean mostrarFechaGeneracion = true;
    private boolean excelCongelarEncabezado  = true;
    private boolean excelFiltrosAutomaticos  = true;
}