package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Opciones que el usuario configura en el modal antes de generar el reporte.
 * Todas las opciones son opcionales — el backend usa defaults seguros.
 */
@Data
public class ReporteAuditoriaConfigDTO {

    // ── Metadatos del reporte ─────────────────────────────────────────────
    /** Título personalizado. Si null usa el default según tipo. */
    private String  titulo;

    /** Subtítulo / descripción adicional. Opcional. */
    private String  subtitulo;

    /** Usuario que genera el reporte — se rellena en el backend desde el JWT */
    private String  generadoPor;

    /** 'PDF' o 'EXCEL' */
    private String  formato;

    /** Solo para PDF: 'VERTICAL' o 'HORIZONTAL' */
    private String  orientacion = "VERTICAL";

    // ── Filtros de datos ──────────────────────────────────────────────────
    /** Fecha de inicio del período. Formato: yyyy-MM-dd */
    private String  desde;

    /** Fecha de fin del período. Formato: yyyy-MM-dd */
    private String  hasta;

    /** Filtro de usuario (parcial, case-insensitive) */
    private String  usuarioApp;

    /** Solo para login: 'SUCCESS' | 'FAIL' | null = todos */
    private String  resultado;

    /** Solo para cambios: nombre de la tabla */
    private String  tabla;

    /** Solo para cambios: 'INSERT' | 'UPDATE' | 'DELETE' | null = todas */
    private String  operacion;

    /** Límite de filas a incluir en el detalle. Default: 500 */
    private Integer limite = 500;

    // ── Secciones a incluir ───────────────────────────────────────────────
    /** Incluir portada con resumen ejecutivo */
    private boolean incluirPortada = true;

    /** Incluir información de la institución en el encabezado */
    private boolean incluirInstitucion = true;

    /** Incluir logo de la institución (solo PDF) */
    private boolean incluirLogo = true;

    /** Incluir sección de KPIs / resumen numérico */
    private boolean incluirKpis = true;

    /** Incluir tabla de detalle registro por registro */
    private boolean incluirDetalle = true;

    /** Incluir gráfico de distribución por tabla (solo cambios, solo Excel) */
    private boolean incluirPorTabla = true;

    /** Incluir top usuarios más activos */
    private boolean incluirTopUsuarios = true;

    /** Incluir cambios externos (sin usuario_app) — solo cambios */
    private boolean incluirExternos = false;

    // ── Opciones visuales (solo PDF) ──────────────────────────────────────
    /** Color primario del reporte en hex. Default: verde UTEQ */
    private String  colorPrimario = "#00A63E";

    /** Mostrar número de página en el pie de página */
    private boolean mostrarNumeroPagina = true;

    /** Mostrar fecha de generación en el pie de página */
    private boolean mostrarFechaGeneracion = true;

    // ── Opciones de Excel ─────────────────────────────────────────────────
    /** Una hoja por sección vs todo en una sola hoja */
    private boolean excelHojasPorSeccion = true;

    /** Congelar fila de encabezados en Excel */
    private boolean excelCongelarEncabezado = true;

    /** Aplicar filtros automáticos en Excel */
    private boolean excelFiltrosAutomaticos = true;

    // ── Tipo de reporte ───────────────────────────────────────────────────
    /** 'LOGIN' | 'CAMBIOS' | 'COMPLETO' */
    private String  tipoReporte = "COMPLETO";
}