package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Configuración completa que el usuario define en el modal
 * antes de generar un reporte de prepostulaciones.
 */
@Data
public class ReportePrepostulacionConfigDTO {

    // ── Metadatos ─────────────────────────────────────────────────────────
    private String  titulo;
    private String  subtitulo;

    /** 'PDF' | 'EXCEL' */
    private String  formato = "PDF";

    /** Solo PDF: 'VERTICAL' | 'HORIZONTAL' */
    private String  orientacion = "VERTICAL";

    // ── Filtros de datos ──────────────────────────────────────────────────
    /** yyyy-MM-dd  — fecha de envío desde */
    private String  desde;

    /** yyyy-MM-dd  — fecha de envío hasta */
    private String  hasta;

    /** IDs de convocatorias seleccionadas; null/vacío = todas */
    private List<Long> idsConvocatoria;

    /** IDs de solicitudes seleccionadas; null/vacío = todas */
    private List<Long> idsSolicitud;

    /** Estado de revisión: 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | null = todos */
    private String  estadoRevision;

    /** Límite de filas en la tabla de detalle. Default 500 */
    private Integer limite = 500;

    // ── Secciones a incluir ───────────────────────────────────────────────
    private boolean incluirPortada           = true;
    private boolean incluirInstitucion       = true;
    private boolean incluirKpis              = true;
    private boolean incluirDetalle           = true;
    private boolean incluirGraficoEstados    = true;   // pastel / barras
    private boolean incluirGraficoTemporal   = true;   // línea por tiempo
    private boolean incluirGraficoConvocatoria = true; // barras por convocatoria

    /** 'PIE' | 'BAR' — tipo de gráfico principal */
    private String  tipoGrafico = "BAR";

    // ── Opciones visuales PDF ─────────────────────────────────────────────
    private String  colorPrimario            = "#00A63E";
    private boolean mostrarNumeroPagina      = true;
    private boolean mostrarFechaGeneracion   = true;

    // ── Opciones Excel ────────────────────────────────────────────────────
    private boolean excelCongelarEncabezado  = true;
    private boolean excelFiltrosAutomaticos  = true;
    private boolean excelHojasPorSeccion     = true;
}