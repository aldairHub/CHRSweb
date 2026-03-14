package org.uteq.backend.service;

// OpenPDF
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
// Apache POI
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
// Spring
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
// Proyecto
import org.uteq.backend.dto.ReportePrepostulacionConfigDTO;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.ConvocatoriaRepository;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.repository.PrepostulacionRepository;
// Lombok
import lombok.RequiredArgsConstructor;
// Java
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportePrepostulacionService {

    private final JdbcTemplate            jdbc;
    private final InstitucionRepository   instRepo;
    private final ConvocatoriaRepository  convRepo;
    private final PrepostulacionRepository prepostRepo;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Color C_APROBADO  = new Color(22,  163,  74);
    private static final Color C_RECHAZADO = new Color(220,  38,  38);
    private static final Color C_PENDIENTE = new Color(217, 119,   6);
    private static final Color C_AZUL      = new Color( 37,  99, 235);
    private static final Color C_VIOLETA   = new Color(168,  85, 247);
    private static final Color C_CELESTE   = new Color( 14, 165, 233);
    private static final Color C_NARANJA   = new Color(249, 115,  22);
    private static final Color[] PALETA    = {
            C_AZUL, C_APROBADO, C_NARANJA, C_VIOLETA, C_CELESTE,
            new Color(236,72,153), new Color(20,184,166), new Color(245,158,11)
    };

    // =========================================================================
    // ENTRADA PRINCIPAL
    // =========================================================================

    public byte[] generar(ReportePrepostulacionConfigDTO cfg) {
        DatosReporte datos = cargarDatos(cfg);
        return "EXCEL".equalsIgnoreCase(cfg.getFormato())
                ? generarExcel(cfg, datos) : generarPdf(cfg, datos);
    }

    public String nombreArchivo(ReportePrepostulacionConfigDTO cfg) {
        String ts  = LocalDateTime.now().format(FMT_NOMBRE);
        String ext = "EXCEL".equalsIgnoreCase(cfg.getFormato()) ? "xlsx" : "pdf";
        return "reporte_prepostulaciones_" + ts + "." + ext;
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================

    private DatosReporte cargarDatos(ReportePrepostulacionConfigDTO cfg) {
        DatosReporte d = new DatosReporte();
        d.institucion   = instRepo.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getActivo())).findFirst().orElse(null);
        d.convocatorias = convRepo.findAllByOrderByFechaPublicacionDesc();
        Date desde = parseDate(cfg.getDesde()), hasta = parseDate(cfg.getHasta());
        d.prepostulaciones = jdbc.queryForList(buildQuery(cfg), buildParams(cfg, desde, hasta).toArray());
        d.kpiEstados       = calcKpiEstados(d.prepostulaciones);
        d.kpiConvocatoria  = calcKpiConvocatoria(d.prepostulaciones);
        d.kpiTemporal      = calcKpiTemporal(d.prepostulaciones);
        d.total      = d.prepostulaciones.size();
        d.aprobadas  = d.kpiEstados.getOrDefault("APROBADO",  0);
        d.rechazadas = d.kpiEstados.getOrDefault("RECHAZADO", 0);
        d.pendientes = d.kpiEstados.getOrDefault("PENDIENTE", 0);
        return d;
    }

    private String buildQuery(ReportePrepostulacionConfigDTO cfg) {
        StringBuilder sb = new StringBuilder(
                "SELECT p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                        " p.correo, p.estado_revision AS \"estadoRevision\"," +
                        " p.fecha_envio AS \"fechaEnvio\", p.fecha_revision AS \"fechaRevision\"," +
                        " p.observaciones_revision AS \"observacionesRevision\"," +
                        " ps.id_solicitud AS \"idSolicitud\"," +
                        " c.id_convocatoria AS \"idConvocatoria\", c.titulo AS \"tituloConvocatoria\"," +
                        " c.estado_convocatoria AS \"estadoConvocatoria\"" +
                        " FROM prepostulacion p" +
                        " LEFT JOIN prepostulacion_solicitud ps ON ps.id_prepostulacion = p.id_prepostulacion" +
                        " LEFT JOIN convocatoria_solicitud cs ON cs.id_solicitud = ps.id_solicitud" +
                        " LEFT JOIN convocatoria c ON c.id_convocatoria = cs.id_convocatoria" +
                        " WHERE 1=1");
        if (ok(cfg.getDesde()))        sb.append(" AND p.fecha_envio >= ?");
        if (ok(cfg.getHasta()))        sb.append(" AND p.fecha_envio <= ?");
        if (ok(cfg.getEstadoRevision()))
            sb.append(" AND UPPER(p.estado_revision) = UPPER(?)");
        if (cfg.getIdsConvocatoria() != null && !cfg.getIdsConvocatoria().isEmpty())
            sb.append(" AND c.id_convocatoria IN (")
                    .append(cfg.getIdsConvocatoria().stream().map(x->"?").collect(Collectors.joining(","))).append(")");
        if (cfg.getIdsSolicitud() != null && !cfg.getIdsSolicitud().isEmpty())
            sb.append(" AND ps.id_solicitud IN (")
                    .append(cfg.getIdsSolicitud().stream().map(x->"?").collect(Collectors.joining(","))).append(")");
        sb.append(" ORDER BY p.fecha_envio DESC");
        if (cfg.getLimite() != null && cfg.getLimite() > 0) sb.append(" LIMIT ").append(cfg.getLimite());
        return sb.toString();
    }

    private List<Object> buildParams(ReportePrepostulacionConfigDTO cfg, Date desde, Date hasta) {
        List<Object> p = new ArrayList<>();
        if (desde != null) p.add(desde);
        if (hasta != null) p.add(hasta);
        if (ok(cfg.getEstadoRevision())) p.add(cfg.getEstadoRevision());
        if (cfg.getIdsConvocatoria() != null) p.addAll(cfg.getIdsConvocatoria());
        if (cfg.getIdsSolicitud()    != null) p.addAll(cfg.getIdsSolicitud());
        return p;
    }

    private Map<String,Integer> calcKpiEstados(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        m.put("PENDIENTE",0); m.put("APROBADO",0); m.put("RECHAZADO",0);
        for (var r : rows) m.merge(str(r.get("estadoRevision")).toUpperCase(), 1, Integer::sum);
        return m;
    }
    private Map<String,Integer> calcKpiConvocatoria(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) { String t = str(r.get("tituloConvocatoria")); if (!"—".equals(t)) m.merge(t,1,Integer::sum); }
        return m.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(a,b)->a,LinkedHashMap::new));
    }
    private Map<String,Integer> calcKpiTemporal(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) { Object f = r.get("fechaEnvio"); if (f!=null) m.merge(f.toString().substring(0,7),1,Integer::sum); }
        return new TreeMap<>(m);
    }

    // =========================================================================
    // PDF  —  gráficos 100% vectoriales con PdfContentByte
    // =========================================================================

    private byte[] generarPdf(ReportePrepostulacionConfigDTO cfg, DatosReporte datos) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            boolean horiz = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            com.lowagie.text.Rectangle pageSz = horiz ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(pageSz, 36, 36, 54, 48);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            Color cp = parseColor(cfg.getColorPrimario());
            Color cpDark = darken(cp, 0.18f);

            // ── Pie de página ────────────────────────────────────────────
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, Document d) {
                    try {
                        PdfContentByte cb = w.getDirectContent();
                        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                        // Línea
                        cb.setColorStroke(new Color(220,220,220));
                        cb.setLineWidth(0.5f);
                        cb.moveTo(d.left(), d.bottom()-5);
                        cb.lineTo(d.right(), d.bottom()-5);
                        cb.stroke();
                        // Texto
                        cb.beginText();
                        cb.setFontAndSize(bf, 7.5f);
                        cb.setColorFill(new Color(155,155,155));
                        String izq = datos.institucion != null && datos.institucion.getNombre() != null
                                ? datos.institucion.getNombre() : "";
                        String der = "";
                        if (cfg.isMostrarFechaGeneracion()) der += LocalDateTime.now().format(FMT_TS) + "  ";
                        if (cfg.isMostrarNumeroPagina())    der += "Pág. " + w.getPageNumber();
                        cb.showTextAligned(Element.ALIGN_LEFT,  izq, d.left(),  d.bottom()-15, 0);
                        cb.showTextAligned(Element.ALIGN_RIGHT, der, d.right(), d.bottom()-15, 0);
                        cb.endText();
                    } catch (Exception ignored) {}
                }
            });

            doc.open();
            float pw = doc.getPageSize().getWidth();
            float ph = doc.getPageSize().getHeight();
            float ml = doc.leftMargin(), mr = doc.rightMargin();
            float contentW = pw - ml - mr;

            // =================================================================
            // PÁGINA 1 — PORTADA
            // =================================================================
            if (cfg.isIncluirPortada()) {
                PdfContentByte cv = writer.getDirectContent();

                // Banda superior
                cv.setColorFill(cp);
                cv.rectangle(0, ph - 120, pw, 120);
                cv.fill();

                // Triángulo decorativo
                cv.setColorFill(cpDark);
                cv.moveTo(pw - 160, ph);
                cv.lineTo(pw, ph);
                cv.lineTo(pw, ph - 160);
                cv.closePath(); cv.fill();

                // Banda inferior
                cv.setColorFill(cpDark);
                cv.rectangle(0, 0, pw, 7); cv.fill();

                // Línea decorativa bajo la banda superior
                cv.setColorFill(new Color(255,255,255,60));
                cv.rectangle(0, ph - 122, pw, 3); cv.fill();

                // Textos sobre banda
                BaseFont bfB  = BaseFont.createFont(BaseFont.HELVETICA_BOLD,  BaseFont.CP1252, false);
                BaseFont bfR  = BaseFont.createFont(BaseFont.HELVETICA,       BaseFont.CP1252, false);

                cv.beginText();
                // Institución
                if (datos.institucion != null && datos.institucion.getNombre() != null) {
                    cv.setFontAndSize(bfR, 9.5f);
                    cv.setColorFill(new Color(255,255,255,190));
                    cv.showTextAligned(Element.ALIGN_LEFT, datos.institucion.getNombre(), ml, ph - 30, 0);
                }
                // Título
                String titulo = ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones";
                cv.setFontAndSize(bfB, 25);
                cv.setColorFill(Color.WHITE);
                cv.showTextAligned(Element.ALIGN_LEFT, titulo, ml, ph - 68, 0);
                // Subtítulo
                if (ok(cfg.getSubtitulo())) {
                    cv.setFontAndSize(bfR, 11);
                    cv.setColorFill(new Color(255,255,255,195));
                    cv.showTextAligned(Element.ALIGN_LEFT, cfg.getSubtitulo(), ml, ph - 90, 0);
                }
                cv.endText();

                // Espacio (banda ocupa ~120pt, márgenes superiores 54pt → texto empieza después)
                doc.add(new Paragraph("\n\n\n\n\n\n\n\n"));

                // Período
                Font fPer = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, new Color(80,80,80));
                String periodo = "Período: "
                        + (ok(cfg.getDesde()) ? cfg.getDesde() : "inicio")
                        + "  →  "
                        + (ok(cfg.getHasta()) ? cfg.getHasta() : "hoy");
                Paragraph pPer = new Paragraph(periodo, fPer);
                pPer.setSpacingBefore(6); pPer.setSpacingAfter(12);
                doc.add(pPer);

                // ── Tarjetas KPI ─────────────────────────────────────────
                PdfPTable kpiTab = new PdfPTable(4);
                kpiTab.setWidthPercentage(100);
                kpiTab.setSpacingAfter(18);
                kpiCard(kpiTab, "TOTAL",      String.valueOf(datos.total),      cp,           new Color(240,255,245));
                kpiCard(kpiTab, "APROBADAS",  String.valueOf(datos.aprobadas),  C_APROBADO,   new Color(240,253,244));
                kpiCard(kpiTab, "RECHAZADAS", String.valueOf(datos.rechazadas), C_RECHAZADO,  new Color(254,242,242));
                kpiCard(kpiTab, "PENDIENTES", String.valueOf(datos.pendientes), C_PENDIENTE,  new Color(255,251,235));
                doc.add(kpiTab);

                // ── Gráfico de pastel vectorial ───────────────────────────
                if (datos.total > 0 && cfg.isIncluirGraficoEstados()) {
                    doc.add(new Paragraph("\n"));
                    // Usamos una tabla para posicionar pastel + leyenda
                    PdfPTable tChart = new PdfPTable(2);
                    tChart.setWidthPercentage(90);
                    tChart.setWidths(new float[]{1, 1});
                    tChart.setHorizontalAlignment(Element.ALIGN_CENTER);

                    // Celda izquierda: pastel dibujado via PdfPCell con evento
                    float pieSize = 160f;
                    PdfPCell cPie = new PdfPCell() {{
                        setFixedHeight(pieSize + 20);
                        setBorder(0);
                        setCellEvent(new PdfPCellEvent() {
                            @Override
                            public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
                                PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
                                float cx = pos.getLeft() + pos.getWidth()  / 2;
                                float cy = pos.getBottom() + pos.getHeight() / 2;
                                float r  = Math.min(pos.getWidth(), pos.getHeight()) / 2 - 8;
                                dibujarPastel(cb, cx, cy, r, datos);
                            }
                        });
                    }};
                    tChart.addCell(cPie);

                    // Celda derecha: leyenda
                    PdfPCell cLey = new PdfPCell();
                    cLey.setBorder(0); cLey.setPaddingLeft(20);
                    cLey.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    addLeyendaEstados(cLey, datos);
                    tChart.addCell(cLey);
                    doc.add(tChart);
                }

                // Fecha generación
                Font fGen = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(160,160,160));
                Paragraph pGen = new Paragraph("Generado el " + LocalDateTime.now().format(FMT_TS), fGen);
                pGen.setAlignment(Element.ALIGN_RIGHT); pGen.setSpacingBefore(16);
                doc.add(pGen);

                doc.newPage();
            }

            // =================================================================
            // PÁGINA 2 — KPIs + Gráficos
            // =================================================================

            // ── Sección KPI ───────────────────────────────────────────────
            if (cfg.isIncluirKpis()) {
                pdfSeccion(doc, "Resumen Estadístico", cp);
                PdfPTable kpiTab = new PdfPTable(4);
                kpiTab.setWidthPercentage(100); kpiTab.setSpacingAfter(14);
                kpiCard(kpiTab, "TOTAL REGISTROS", String.valueOf(datos.total),      cp,           new Color(240,255,245));
                kpiCard(kpiTab, "APROBADAS",        String.valueOf(datos.aprobadas),  C_APROBADO,   new Color(240,253,244));
                kpiCard(kpiTab, "RECHAZADAS",        String.valueOf(datos.rechazadas), C_RECHAZADO,  new Color(254,242,242));
                kpiCard(kpiTab, "PENDIENTES",        String.valueOf(datos.pendientes), C_PENDIENTE,  new Color(255,251,235));
                doc.add(kpiTab);
            }

            // ── Gráfico de pastel estados ─────────────────────────────────
            if (cfg.isIncluirGraficoEstados() && datos.total > 0) {
                pdfSeccion(doc, "Distribución por Estado de Revisión", cp);
                PdfPTable tChart = new PdfPTable(2);
                tChart.setWidthPercentage(90);
                tChart.setWidths(new float[]{1, 1});
                tChart.setHorizontalAlignment(Element.ALIGN_CENTER);
                tChart.setSpacingAfter(14);

                PdfPCell cPie = new PdfPCell();
                cPie.setFixedHeight(200);
                cPie.setBorder(0);
                cPie.setCellEvent(new PdfPCellEvent() {
                    @Override public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
                        PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
                        float cx = pos.getLeft() + pos.getWidth()  / 2;
                        float cy = pos.getBottom() + pos.getHeight() / 2;
                        float r  = Math.min(pos.getWidth(), pos.getHeight()) / 2 - 10;
                        dibujarPastel(cb, cx, cy, r, datos);
                    }
                });
                tChart.addCell(cPie);

                PdfPCell cLey = new PdfPCell();
                cLey.setBorder(0); cLey.setPaddingLeft(20);
                cLey.setVerticalAlignment(Element.ALIGN_MIDDLE);
                addLeyendaEstados(cLey, datos);
                tChart.addCell(cLey);
                doc.add(tChart);
            }

            // ── Gráfico de barras por convocatoria ────────────────────────
            if (cfg.isIncluirGraficoConvocatoria() && !datos.kpiConvocatoria.isEmpty()) {
                pdfSeccion(doc, "Prepostulaciones por Convocatoria", cp);
                int entries = Math.min(datos.kpiConvocatoria.size(), 8);
                float barH = 26f, gap = 8f;
                float totalH = entries * (barH + gap) + 20;
                PdfPTable tBars = new PdfPTable(1);
                tBars.setWidthPercentage(100);
                tBars.setSpacingAfter(14);
                PdfPCell cBars = new PdfPCell();
                cBars.setFixedHeight(totalH);
                cBars.setBorder(0);
                cBars.setCellEvent(new PdfPCellEvent() {
                    @Override public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
                        PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
                        dibujarBarrasH(cb, pos, datos.kpiConvocatoria, barH, gap);
                    }
                });
                tBars.addCell(cBars);
                doc.add(tBars);
            }

            // ── Gráfico de línea temporal ─────────────────────────────────
            if (cfg.isIncluirGraficoTemporal() && !datos.kpiTemporal.isEmpty()) {
                pdfSeccion(doc, "Evolución Temporal de Prepostulaciones", cp);
                PdfPTable tLine = new PdfPTable(1);
                tLine.setWidthPercentage(100);
                tLine.setSpacingAfter(14);
                PdfPCell cLine = new PdfPCell();
                cLine.setFixedHeight(200);
                cLine.setBorder(0);
                cLine.setCellEvent(new PdfPCellEvent() {
                    @Override public void cellLayout(PdfPCell cell, Rectangle pos, PdfContentByte[] canvases) {
                        PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
                        dibujarLineaTemporal(cb, pos, datos.kpiTemporal, cp);
                    }
                });
                tLine.addCell(cLine);
                doc.add(tLine);
            }

            // =================================================================
            // PÁGINA DETALLE
            // =================================================================
            if (cfg.isIncluirDetalle() && !datos.prepostulaciones.isEmpty()) {
                doc.newPage();
                pdfSeccion(doc, "Detalle de Prepostulaciones (" + datos.prepostulaciones.size() + " registros)", cp);
                pdfTablaDetalle(doc, datos.prepostulaciones, cp);
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // GRÁFICOS VECTORIALES — PdfContentByte puro
    // =========================================================================

    /**
     * Pastel tipo donut dibujado con arcos y sectores de OpenPDF.
     * cx, cy = centro en coordenadas absolutas del canvas; r = radio exterior.
     */
    private void dibujarPastel(PdfContentByte cb, float cx, float cy, float r, DatosReporte datos) {
        Color[] cols  = {C_PENDIENTE, C_APROBADO, C_RECHAZADO};
        String[] keys = {"PENDIENTE", "APROBADO", "RECHAZADO"};
        int[] vals = {
                datos.kpiEstados.getOrDefault("PENDIENTE",  0),
                datos.kpiEstados.getOrDefault("APROBADO",   0),
                datos.kpiEstados.getOrDefault("RECHAZADO",  0)
        };
        int total = datos.total == 0 ? 1 : datos.total;
        float innerR = r * 0.40f; // radio del hueco donut
        int N = 60; // segmentos por arco

        double startDeg = 90.0; // empieza desde arriba

        for (int i = 0; i < 3; i++) {
            if (vals[i] == 0) continue;
            double sweep  = 360.0 * vals[i] / total;
            double endDeg = startDeg - sweep;

            // Dibujar sector (polígono con arco exterior e interior)
            cb.setColorFill(cols[i]);
            cb.setColorStroke(Color.WHITE);
            cb.setLineWidth(1.5f);

            // Construir sector desde startDeg hasta endDeg
            cb.moveTo(
                    cx + (float)(r * Math.cos(Math.toRadians(startDeg))),
                    cy + (float)(r * Math.sin(Math.toRadians(startDeg)))
            );
            // Arco exterior
            for (int s = 0; s <= N; s++) {
                double a = Math.toRadians(startDeg - sweep * s / N);
                cb.lineTo(cx + (float)(r * Math.cos(a)), cy + (float)(r * Math.sin(a)));
            }
            // Hacia el borde interior
            cb.lineTo(
                    cx + (float)(innerR * Math.cos(Math.toRadians(endDeg))),
                    cy + (float)(innerR * Math.sin(Math.toRadians(endDeg)))
            );
            // Arco interior en sentido contrario
            for (int s = 0; s <= N; s++) {
                double a = Math.toRadians(endDeg + sweep * s / N);
                cb.lineTo(cx + (float)(innerR * Math.cos(a)), cy + (float)(innerR * Math.sin(a)));
            }
            cb.closePath();
            cb.fillStroke();

            // Etiqueta de porcentaje (si el sector es grande)
            if (sweep > 15) {
                double midA = Math.toRadians(startDeg - sweep / 2);
                float tx = cx + (float)((r * 0.65f) * Math.cos(midA));
                float ty = cy + (float)((r * 0.65f) * Math.sin(midA));
                String pctStr = String.format("%.0f%%", 100.0 * vals[i] / total);
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                    cb.beginText();
                    cb.setFontAndSize(bf, 10);
                    cb.setColorFill(Color.WHITE);
                    cb.showTextAligned(Element.ALIGN_CENTER, pctStr, tx, ty - 4, 0);
                    cb.endText();
                } catch (Exception ignored) {}
            }

            startDeg = endDeg;
        }

        // Círculo interior (hueco del donut) con total
        cb.setColorFill(Color.WHITE);
        cb.setColorStroke(new Color(230,230,230));
        cb.setLineWidth(0.5f);
        // Dibujar círculo interior
        cb.arc(cx - innerR, cy - innerR, cx + innerR, cy + innerR, 0, 360);
        cb.fillStroke();

        // Texto del total en el centro
        try {
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfR = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            cb.beginText();
            cb.setColorFill(new Color(40,40,40));
            cb.setFontAndSize(bfB, 16);
            cb.showTextAligned(Element.ALIGN_CENTER, String.valueOf(datos.total), cx, cy + 2, 0);
            cb.setFontAndSize(bfR, 8);
            cb.setColorFill(new Color(130,130,130));
            cb.showTextAligned(Element.ALIGN_CENTER, "total", cx, cy - 10, 0);
            cb.endText();
        } catch (Exception ignored) {}
    }

    /**
     * Barras horizontales dibujadas vectorialmente con OpenPDF.
     */
    private void dibujarBarrasH(PdfContentByte cb, Rectangle pos,
                                Map<String,Integer> data, float barH, float gap) {
        if (data.isEmpty()) return;
        int maxVal = data.values().stream().max(Integer::compareTo).orElse(1);
        float labelW = 170f;
        float barAreaW = pos.getWidth() - labelW - 45f;
        float x0 = pos.getLeft();
        float y = pos.getTop() - 10;

        try {
            BaseFont bf  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            int idx = 0;
            for (var entry : data.entrySet()) {
                if (idx >= 8) break;
                Color barC = PALETA[idx % PALETA.length];
                int cant   = entry.getValue();
                float barW = (float) cant / maxVal * barAreaW;
                float yBar = y - barH;

                // Fondo alterno de fila
                if (idx % 2 == 0) {
                    cb.setColorFill(new Color(248,249,250));
                    cb.rectangle(x0, yBar - 3, pos.getWidth(), barH + 6);
                    cb.fill();
                }

                // Etiqueta izquierda
                cb.beginText();
                cb.setFontAndSize(bf, 8.5f);
                cb.setColorFill(new Color(55,55,55));
                cb.showTextAligned(Element.ALIGN_LEFT, truncar(entry.getKey(), 27), x0 + 4, yBar + barH / 3, 0);
                cb.endText();

                // Barra con sombra sutil
                if (barW > 0) {
                    cb.setColorFill(new Color(barC.getRed(), barC.getGreen(), barC.getBlue(), 40));
                    cb.rectangle(x0 + labelW + 2, yBar - 2, barW, barH);
                    cb.fill();

                    cb.setColorFill(barC);
                    cb.rectangle(x0 + labelW, yBar, barW, barH);
                    cb.fill();

                    // Brillo superior
                    cb.setColorFill(new Color(255,255,255,50));
                    cb.rectangle(x0 + labelW, yBar + barH * 0.6f, barW, barH * 0.35f);
                    cb.fill();
                }

                // Valor al final de la barra
                cb.beginText();
                cb.setFontAndSize(bfB, 9);
                cb.setColorFill(new Color(50,50,50));
                cb.showTextAligned(Element.ALIGN_LEFT, String.valueOf(cant),
                        x0 + labelW + barW + 6, yBar + barH / 3, 0);
                cb.endText();

                y -= (barH + gap);
                idx++;
            }
        } catch (Exception ignored) {}
    }

    /**
     * Gráfico de línea temporal vectorial.
     */
    private void dibujarLineaTemporal(PdfContentByte cb, Rectangle pos,
                                      Map<String,Integer> data, Color cp) {
        if (data.isEmpty()) return;
        List<String>  meses = new ArrayList<>(data.keySet());
        List<Integer> vals  = new ArrayList<>(data.values());
        int n    = meses.size();
        int maxV = vals.stream().max(Integer::compareTo).orElse(1);

        float padL = 45, padR = 20, padT = 15, padB = 35;
        float chartX = pos.getLeft()   + padL;
        float chartY = pos.getBottom() + padB;
        float chartW = pos.getWidth()  - padL - padR;
        float chartH = pos.getHeight() - padT - padB;

        try {
            BaseFont bf  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            // Fondo claro
            cb.setColorFill(new Color(250,251,252));
            cb.rectangle(chartX, chartY, chartW, chartH);
            cb.fill();

            // Cuadrícula horizontal
            int gridLines = 4;
            for (int gi = 0; gi <= gridLines; gi++) {
                float gv = maxV * gi / (float) gridLines;
                float gy = chartY + chartH * gi / gridLines;
                cb.setColorStroke(new Color(220,220,220));
                cb.setLineWidth(0.5f);
                cb.moveTo(chartX, gy); cb.lineTo(chartX + chartW, gy); cb.stroke();
                cb.beginText();
                cb.setFontAndSize(bf, 7.5f);
                cb.setColorFill(new Color(130,130,130));
                cb.showTextAligned(Element.ALIGN_RIGHT, String.valueOf((int)gv), chartX - 5, gy - 3, 0);
                cb.endText();
            }

            // Eje X
            cb.setColorStroke(new Color(180,180,180));
            cb.setLineWidth(1f);
            cb.moveTo(chartX, chartY); cb.lineTo(chartX + chartW, chartY); cb.stroke();

            // Calcular puntos
            float[] px = new float[n], py = new float[n];
            for (int i = 0; i < n; i++) {
                px[i] = chartX + (n == 1 ? chartW / 2 : (float) i / (n - 1) * chartW);
                py[i] = chartY + (float) vals.get(i) / maxV * chartH;
            }

            // Área rellena bajo la línea
            if (n > 1) {
                cb.setColorFill(new Color(cp.getRed(), cp.getGreen(), cp.getBlue(), 30));
                cb.moveTo(px[0], chartY);
                for (int i = 0; i < n; i++) cb.lineTo(px[i], py[i]);
                cb.lineTo(px[n-1], chartY);
                cb.closePath(); cb.fill();
            }

            // Línea principal
            cb.setColorStroke(cp);
            cb.setLineWidth(2.5f);
            cb.moveTo(px[0], py[0]);
            for (int i = 1; i < n; i++) cb.lineTo(px[i], py[i]);
            cb.stroke();

            // Puntos, valores y etiquetas de mes
            for (int i = 0; i < n; i++) {
                // Punto exterior
                cb.setColorFill(cp);
                cb.circle(px[i], py[i], 5); cb.fill();
                // Punto interior blanco
                cb.setColorFill(Color.WHITE);
                cb.circle(px[i], py[i], 3); cb.fill();

                // Valor encima
                cb.beginText();
                cb.setFontAndSize(bfB, 8.5f);
                cb.setColorFill(new Color(50,50,50));
                cb.showTextAligned(Element.ALIGN_CENTER, String.valueOf(vals.get(i)), px[i], py[i] + 8, 0);
                cb.endText();

                // Etiqueta del mes
                cb.beginText();
                cb.setFontAndSize(bf, 7.5f);
                cb.setColorFill(new Color(100,100,100));
                String mes = meses.get(i).length() > 7 ? meses.get(i).substring(2) : meses.get(i);
                cb.showTextAligned(Element.ALIGN_CENTER, mes, px[i], chartY - 14, 0);
                cb.endText();
            }

        } catch (Exception ignored) {}
    }

    // ── Leyenda de estados ────────────────────────────────────────────────────
    private void addLeyendaEstados(PdfPCell cell, DatosReporte datos) {
        int total = datos.total == 0 ? 1 : datos.total;
        Color[]  cols  = {C_PENDIENTE, C_APROBADO, C_RECHAZADO};
        String[] keys  = {"PENDIENTE", "APROBADO", "RECHAZADO"};
        Font fTit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, new Color(50,50,50));
        Font fSub = FontFactory.getFont(FontFactory.HELVETICA,      8f,   new Color(100,100,100));
        cell.addElement(new Paragraph("Distribución de\nprepostulaciones", fTit));
        cell.addElement(new Paragraph("\n"));
        for (int i = 0; i < keys.length; i++) {
            int cant = datos.kpiEstados.getOrDefault(keys[i], 0);
            double pct = cant * 100.0 / total;
            PdfPTable row = new PdfPTable(new float[]{6, 60, 34});
            row.setWidthPercentage(100); row.setSpacingBefore(6);
            PdfPCell dot = new PdfPCell(new Phrase(" "));
            dot.setBackgroundColor(cols[i]); dot.setBorder(0); dot.setFixedHeight(13); row.addCell(dot);
            PdfPCell lbl = new PdfPCell(); lbl.setBorder(0); lbl.setPaddingLeft(6);
            lbl.addElement(new Paragraph(keys[i], fSub));
            lbl.addElement(new Paragraph(cant + " registro" + (cant != 1 ? "s" : ""), fSub));
            row.addCell(lbl);
            Font fPct = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, cols[i]);
            PdfPCell cp2 = new PdfPCell(new Phrase(String.format("%.1f%%", pct), fPct));
            cp2.setBorder(0); cp2.setVerticalAlignment(Element.ALIGN_MIDDLE); row.addCell(cp2);
            cell.addElement(row);
        }
    }

    // ── Tarjeta KPI ───────────────────────────────────────────────────────────
    private void kpiCard(PdfPTable t, String label, String valor, Color color, Color bg) {
        Font fL = FontFactory.getFont(FontFactory.HELVETICA,      7.5f, new Color(120,120,120));
        Font fV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22f,  color);
        // Layout: barra lateral de color | contenido
        PdfPTable inner = new PdfPTable(new float[]{4, 96});
        inner.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell(new Phrase(" "));
        bar.setBackgroundColor(color); bar.setBorder(0); bar.setPadding(0);
        inner.addCell(bar);
        PdfPCell con = new PdfPCell(); con.setBorder(0); con.setPaddingLeft(10); con.setPadding(10);
        con.addElement(new Paragraph(label, fL));
        con.addElement(new Paragraph(valor, fV));
        inner.addCell(con);
        PdfPCell card = new PdfPCell();
        card.addElement(inner);
        card.setBackgroundColor(bg);
        card.setBorderColor(new Color(220,220,220)); card.setBorderWidth(1f);
        card.setPadding(0);
        t.addCell(card);
    }

    // ── Sección encabezado ────────────────────────────────────────────────────
    private void pdfSeccion(Document doc, String titulo, Color cp) throws DocumentException {
        doc.add(new Paragraph("\n"));
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, cp);
        Paragraph p = new Paragraph(titulo, f); p.setSpacingAfter(4); doc.add(p);
        doc.add(new Chunk(new LineSeparator(1.5f, 100, cp, Element.ALIGN_CENTER, 0)));
        doc.add(new Paragraph("\n"));
    }

    // ── Tabla de detalle ──────────────────────────────────────────────────────
    private void pdfTablaDetalle(Document doc, List<Map<String,Object>> rows, Color cp)
            throws DocumentException {
        String[] hdrs = {"ID","Nombres","Apellidos","Cédula","Estado","Fecha Envío","Convocatoria"};
        float[]  ws   = {6, 17, 17, 13, 11, 13, 23};
        PdfPTable t   = new PdfPTable(hdrs.length);
        t.setWidthPercentage(100); t.setWidths(ws); t.setSpacingBefore(6);
        Font fH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.WHITE);
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(cp); c.setPadding(6); c.setBorderColor(cp); t.addCell(c);
        }
        Font fR = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(50,50,50));
        boolean alt = false;
        Color bgAlt = new Color(249,250,251);
        for (var row : rows) {
            Color bg = alt ? bgAlt : Color.WHITE; alt = !alt;
            addCell(t, str(row.get("id_prepostulacion")),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(50,50,50)), bg);
            addCell(t, str(row.get("nombres")),          fR, bg);
            addCell(t, str(row.get("apellidos")),        fR, bg);
            addCell(t, str(row.get("identificacion")),   fR, bg);
            String estado = str(row.get("estadoRevision")).toUpperCase();
            Color bgE = bg; Font fE = fR;
            if ("APROBADO".equals(estado))  { bgE = new Color(220,252,231); fE = FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(21,128,61)); }
            if ("RECHAZADO".equals(estado)) { bgE = new Color(254,226,226); fE = FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(185,28,28)); }
            if ("PENDIENTE".equals(estado)) { bgE = new Color(254,249,195); fE = FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(146,64,14)); }
            addCell(t, estado, fE, bgE);
            addCell(t, formatFecha(row.get("fechaEnvio")),                  fR, bg);
            addCell(t, truncar(str(row.get("tituloConvocatoria")), 28),     fR, bg);
        }
        doc.add(t);
    }

    private void addCell(PdfPTable t, String v, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "—", f));
        c.setPadding(4.5f); c.setBackgroundColor(bg);
        c.setBorderColor(new Color(230,230,230)); c.setBorderWidth(0.5f);
        t.addCell(c);
    }

    // =========================================================================
    // EXCEL — tablas completas y ricas
    // =========================================================================

    private byte[] generarExcel(ReportePrepostulacionConfigDTO cfg, DatosReporte datos) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Color cp = parseColor(cfg.getColorPrimario());
            XlsEstilos ex = new XlsEstilos(wb, cp);

            xlsResumen(wb.createSheet("Resumen"), cfg, datos, ex, wb, cp);
            if (cfg.isIncluirGraficoEstados())
                xlsPorEstado(wb.createSheet("Por Estado"), datos, ex, wb);
            if (cfg.isIncluirGraficoConvocatoria() && !datos.kpiConvocatoria.isEmpty())
                xlsPorConvocatoria(wb.createSheet("Por Convocatoria"), datos, ex, wb);
            if (cfg.isIncluirGraficoTemporal() && !datos.kpiTemporal.isEmpty())
                xlsTemporal(wb.createSheet("Evolucion Temporal"), datos, ex, wb);
            if (cfg.isIncluirDetalle() && !datos.prepostulaciones.isEmpty())
                xlsDetalle(wb.createSheet("Detalle"), datos.prepostulaciones, cfg, ex, wb);

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel", e); }
    }

    private void xlsResumen(XSSFSheet sh, ReportePrepostulacionConfigDTO cfg,
                            DatosReporte datos, XlsEstilos ex, XSSFWorkbook wb, Color cp) {
        int f = 0;
        Row rT = sh.createRow(f++); rT.setHeightInPoints(36);
        Cell cT = rT.createCell(0);
        cT.setCellValue(ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones");
        cT.setCellStyle(ex.titulo); sh.addMergedRegion(new CellRangeAddress(0,0,0,5));
        Row rP = sh.createRow(f++); Cell cP = rP.createCell(0);
        cP.setCellValue("Período: "+(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy"));
        cP.setCellStyle(ex.subInfo); sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5)); f++;

        String[] kL = {"Total Prepostulaciones","Aprobadas","Rechazadas","Pendientes"};
        int[]    kV = {datos.total, datos.aprobadas, datos.rechazadas, datos.pendientes};
        Color[]  kC = {cp, C_APROBADO, C_RECHAZADO, C_PENDIENTE};
        Row rHK = sh.createRow(f++);
        for (int i=0;i<4;i++){Cell c=rHK.createCell(i);c.setCellValue(kL[i]);c.setCellStyle(xlsColorHdr(wb,kC[i]));}
        Row rVK = sh.createRow(f++); rVK.setHeightInPoints(30);
        for (int i=0;i<4;i++){Cell c=rVK.createCell(i);c.setCellValue(kV[i]);c.setCellStyle(xlsKpiNum(wb,kC[i]));}
        f+=2;

        // Distribución por estado
        Row rHE = sh.createRow(f++);
        xlsHdrRow(rHE, new String[]{"Estado","Cantidad","% del Total","Barra visual"}, ex);
        sh.addMergedRegion(new CellRangeAddress(f-1,f-1,3,5));
        int tot = datos.total==0?1:datos.total;
        Color[] eCols = {C_PENDIENTE,C_APROBADO,C_RECHAZADO};
        String[] eKeys = {"PENDIENTE","APROBADO","RECHAZADO"};
        for (int i=0;i<3;i++){
            int v=datos.kpiEstados.getOrDefault(eKeys[i],0);
            double pct=v*100.0/tot;
            Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(eKeys[i]);
            r.createCell(1).setCellValue(v);
            r.createCell(2).setCellValue(String.format("%.1f%%",pct));
            int bars=(int)(pct/5);
            Cell cB=r.createCell(3);
            cB.setCellValue("█".repeat(Math.max(bars,0))+"░".repeat(Math.max(20-bars,0)));
            cB.setCellStyle(xlsColorTxt(wb,eCols[i]));
            sh.addMergedRegion(new CellRangeAddress(f-1,f-1,3,5));
        }
        f+=2;
        Row rG=sh.createRow(f); Cell cG=rG.createCell(0);
        cG.setCellValue("Generado: "+LocalDateTime.now().format(FMT_TS));
        cG.setCellStyle(ex.subInfo); sh.addMergedRegion(new CellRangeAddress(f,f,0,5));
        sh.setColumnWidth(0,8000);sh.setColumnWidth(1,4500);sh.setColumnWidth(2,4500);
        sh.setColumnWidth(3,9000);sh.setColumnWidth(4,3000);sh.setColumnWidth(5,3000);
    }

    private void xlsPorEstado(XSSFSheet sh, DatosReporte datos, XlsEstilos ex, XSSFWorkbook wb) {
        int f=0;
        Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0);
        cT.setCellValue("Distribución por Estado de Revisión"); cT.setCellStyle(ex.titulo);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++);
        xlsHdrRow(hdr,new String[]{"Estado","Cantidad","Porcentaje","Proporción visual","Descripción"},ex);
        int tot=datos.total==0?1:datos.total;
        Color[] cols={C_PENDIENTE,C_APROBADO,C_RECHAZADO};
        String[] keys={"PENDIENTE","APROBADO","RECHAZADO"};
        String[] descs={"En espera de revisión","Documentos aprobados","Documentos rechazados"};
        for (int i=0;i<3;i++){
            int v=datos.kpiEstados.getOrDefault(keys[i],0); double pct=v*100.0/tot;
            Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(keys[i]); r.createCell(1).setCellValue(v);
            r.createCell(2).setCellValue(String.format("%.2f%%",pct));
            int bars=(int)(pct/5);
            Cell cB=r.createCell(3); cB.setCellValue("█".repeat(Math.max(bars,0))+"░".repeat(Math.max(20-bars,0))); cB.setCellStyle(xlsColorTxt(wb,cols[i]));
            r.createCell(4).setCellValue(descs[i]);
        }
        f++;
        Row rTot=sh.createRow(f);
        Cell l=rTot.createCell(0);l.setCellValue("TOTAL");l.setCellStyle(ex.header);
        Cell v2=rTot.createCell(1);v2.setCellValue(datos.total);v2.setCellStyle(ex.header);
        Cell p2=rTot.createCell(2);p2.setCellValue("100.00%");p2.setCellStyle(ex.header);
        for(int i=0;i<5;i++) sh.autoSizeColumn(i); sh.setColumnWidth(3,9000);
    }

    private void xlsPorConvocatoria(XSSFSheet sh, DatosReporte datos, XlsEstilos ex, XSSFWorkbook wb) {
        int f=0;
        Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0);
        cT.setCellValue("Prepostulaciones por Convocatoria"); cT.setCellStyle(ex.titulo);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++);
        xlsHdrRow(hdr,new String[]{"Pos.","Convocatoria","N° Prepostulaciones","% del Total","Ranking visual"},ex);
        int tot=datos.total==0?1:datos.total;
        int maxV=datos.kpiConvocatoria.values().stream().max(Integer::compareTo).orElse(1);
        int pos=1;
        for(var e:datos.kpiConvocatoria.entrySet()){
            double pct=e.getValue()*100.0/tot; int bars=(int)(e.getValue()*1.0/maxV*20);
            Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(pos); r.createCell(1).setCellValue(e.getKey());
            r.createCell(2).setCellValue(e.getValue()); r.createCell(3).setCellValue(String.format("%.1f%%",pct));
            Cell cB=r.createCell(4); cB.setCellValue("█".repeat(Math.max(bars,0))); cB.setCellStyle(xlsColorTxt(wb,PALETA[(pos-1)%PALETA.length]));
            pos++;
        }
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),13000));
        sh.autoSizeColumn(2);sh.autoSizeColumn(3);sh.setColumnWidth(4,8000);
    }

    private void xlsTemporal(XSSFSheet sh, DatosReporte datos, XlsEstilos ex, XSSFWorkbook wb) {
        int f=0;
        Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0);
        cT.setCellValue("Evolución Temporal de Prepostulaciones"); cT.setCellStyle(ex.titulo);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++);
        xlsHdrRow(hdr,new String[]{"Mes","N° Prepostulaciones","Tendencia","Δ vs mes anterior","Acumulado"},ex);
        List<String> meses=new ArrayList<>(datos.kpiTemporal.keySet());
        List<Integer> vals=new ArrayList<>(datos.kpiTemporal.values());
        int maxV=vals.stream().max(Integer::compareTo).orElse(1), acum=0;
        for(int i=0;i<meses.size();i++){
            int v=vals.get(i); acum+=v; int prev=i>0?vals.get(i-1):0, delta=i>0?v-prev:0;
            Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(meses.get(i)); r.createCell(1).setCellValue(v);
            int bars=(int)(v*1.0/maxV*20);
            Cell cB=r.createCell(2); cB.setCellValue("█".repeat(Math.max(bars,0))); cB.setCellStyle(xlsColorTxt(wb,C_AZUL));
            Cell cD=r.createCell(3);
            if(i==0){cD.setCellValue("—");}else{cD.setCellValue((delta>=0?"▲ +":"▼ ")+delta);cD.setCellStyle(xlsColorTxt(wb,delta>=0?C_APROBADO:C_RECHAZADO));}
            r.createCell(4).setCellValue(acum);
        }
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(2,9000);sh.autoSizeColumn(3);sh.autoSizeColumn(4);
    }

    private void xlsDetalle(XSSFSheet sh, List<Map<String,Object>> rows,
                            ReportePrepostulacionConfigDTO cfg, XlsEstilos ex, XSSFWorkbook wb) {
        String[] hdrs={"ID","Nombres","Apellidos","Identificación","Correo","Estado",
                "Fecha Envío","Fecha Revisión","Convocatoria","ID Solicitud","Observaciones"};
        Row rH=sh.createRow(0);
        for(int i=0;i<hdrs.length;i++){Cell c=rH.createCell(i);c.setCellValue(hdrs[i]);c.setCellStyle(ex.header);}
        XSSFCellStyle stA=xlsBg(wb,new Color(220,252,231)), stR=xlsBg(wb,new Color(254,226,226)), stP=xlsBg(wb,new Color(254,249,195));
        int f=1;
        for(var row:rows){
            Row r=sh.createRow(f++); int col=0;
            r.createCell(col++).setCellValue(str(row.get("id_prepostulacion")));
            r.createCell(col++).setCellValue(str(row.get("nombres")));
            r.createCell(col++).setCellValue(str(row.get("apellidos")));
            r.createCell(col++).setCellValue(str(row.get("identificacion")));
            r.createCell(col++).setCellValue(str(row.get("correo")));
            Cell cE=r.createCell(col++); String est=str(row.get("estadoRevision")).toUpperCase();
            cE.setCellValue(est);
            if("APROBADO".equals(est))cE.setCellStyle(stA); else if("RECHAZADO".equals(est))cE.setCellStyle(stR); else if("PENDIENTE".equals(est))cE.setCellStyle(stP);
            r.createCell(col++).setCellValue(formatFecha(row.get("fechaEnvio")));
            r.createCell(col++).setCellValue(formatFecha(row.get("fechaRevision")));
            r.createCell(col++).setCellValue(str(row.get("tituloConvocatoria")));
            r.createCell(col++).setCellValue(str(row.get("idSolicitud")));
            r.createCell(col).setCellValue(str(row.get("observacionesRevision")));
        }
        for(int i=0;i<hdrs.length;i++) sh.autoSizeColumn(i);
        sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),7000));
        sh.setColumnWidth(2,Math.max(sh.getColumnWidth(2),7000));
        sh.setColumnWidth(8,Math.max(sh.getColumnWidth(8),11000));
        sh.setColumnWidth(10,Math.max(sh.getColumnWidth(10),11000));
        if(cfg.isExcelCongelarEncabezado()) sh.createFreezePane(0,1);
        if(cfg.isExcelFiltrosAutomaticos()) sh.setAutoFilter(new CellRangeAddress(0,0,0,hdrs.length-1));
    }

    // ── Helpers Excel ─────────────────────────────────────────────────────────
    private void xlsHdrRow(Row r, String[] hdrs, XlsEstilos ex) {
        for(int i=0;i<hdrs.length;i++){Cell c=r.createCell(i);c.setCellValue(hdrs[i]);c.setCellStyle(ex.header);}
    }
    private XSSFCellStyle xlsColorHdr(XSSFWorkbook wb, Color color) {
        XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont();
        f.setBold(true);f.setFontHeightInPoints((short)9);f.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);s.setAlignment(HorizontalAlignment.CENTER);return s;
    }
    private XSSFCellStyle xlsKpiNum(XSSFWorkbook wb, Color color) {
        XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont();
        f.setBold(true);f.setFontHeightInPoints((short)16);f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);s.setVerticalAlignment(VerticalAlignment.CENTER);return s;
    }
    private XSSFCellStyle xlsColorTxt(XSSFWorkbook wb, Color color) {
        XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont();
        f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);return s;
    }
    private XSSFCellStyle xlsBg(XSSFWorkbook wb, Color bg) {
        XSSFCellStyle s=wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s;
    }

    private static class XlsEstilos {
        final XSSFCellStyle titulo,seccion,label,valor,subInfo,header;
        XlsEstilos(XSSFWorkbook wb, Color cp) {
            XSSFFont fT=wb.createFont();fT.setBold(true);fT.setFontHeightInPoints((short)16);fT.setColor(new XSSFColor(cp,new DefaultIndexedColorMap()));
            XSSFFont fH=wb.createFont();fH.setBold(true);fH.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));
            titulo =mk(wb,fT,null,false);seccion=mk(wb,null,null,true);label=mk(wb,null,null,false);
            valor  =mk(wb,null,null,true);subInfo=mk(wb,null,null,false);
            XSSFCellStyle hs=wb.createCellStyle();hs.setFont(fH);hs.setFillForegroundColor(new XSSFColor(cp,new DefaultIndexedColorMap()));
            hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);hs.setAlignment(HorizontalAlignment.CENTER);header=hs;
        }
        private static XSSFCellStyle mk(XSSFWorkbook wb, XSSFFont font, Color bg, boolean bold) {
            XSSFCellStyle s=wb.createCellStyle();
            if(font!=null)s.setFont(font); else if(bold){XSSFFont f=wb.createFont();f.setBold(true);s.setFont(f);}
            if(bg!=null){s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}
            return s;
        }
    }

    // =========================================================================
    // HELPERS GENERALES
    // =========================================================================
    private static class DatosReporte {
        Institucion institucion; List<Convocatoria> convocatorias;
        List<Map<String,Object>> prepostulaciones = new ArrayList<>();
        Map<String,Integer> kpiEstados=new LinkedHashMap<>(), kpiConvocatoria=new LinkedHashMap<>(), kpiTemporal=new LinkedHashMap<>();
        int total, aprobadas, rechazadas, pendientes;
    }

    private Date  parseDate(String s) { if(s==null||s.isBlank())return null; try{return Date.valueOf(LocalDate.parse(s));}catch(Exception e){return null;} }
    private String str(Object o) { return o!=null?o.toString():"—"; }
    private boolean ok(String s) { return s!=null&&!s.isBlank(); }
    private String truncar(String s, int max) { if(s==null||"—".equals(s))return"—"; return s.length()>max?s.substring(0,max)+"…":s; }
    private String formatFecha(Object o) { if(o==null)return"—"; try{return LocalDate.parse(o.toString().substring(0,10)).format(FMT_FECHA);}catch(Exception e){return o.toString();} }
    private Color parseColor(String hex) { if(hex==null||hex.isBlank())return new Color(0,166,62); try{hex=hex.startsWith("#")?hex.substring(1):hex;return new Color(Integer.parseInt(hex.substring(0,2),16),Integer.parseInt(hex.substring(2,4),16),Integer.parseInt(hex.substring(4,6),16));}catch(Exception e){return new Color(0,166,62);} }
    private Color darken(Color c, float f) { return new Color(Math.max(0,(int)(c.getRed()*(1-f))),Math.max(0,(int)(c.getGreen()*(1-f))),Math.max(0,(int)(c.getBlue()*(1-f)))); }
}