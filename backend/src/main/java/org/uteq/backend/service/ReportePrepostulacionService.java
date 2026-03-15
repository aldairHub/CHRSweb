package org.uteq.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.ReportePrepostulacionConfigDTO;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.ConvocatoriaRepository;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.repository.PrepostulacionRepository;
import lombok.RequiredArgsConstructor;
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

    private final JdbcTemplate             jdbc;
    private final InstitucionRepository    instRepo;
    private final ConvocatoriaRepository   convRepo;
    private final PrepostulacionRepository prepostRepo;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ── Paleta minimalista ────────────────────────────────────────────────────
    private static final Color C_APROBADO  = new Color( 22, 163,  74);
    private static final Color C_RECHAZADO = new Color(220,  38,  38);
    private static final Color C_PENDIENTE = new Color(202, 138,   4);
    private static final Color C_AZUL      = new Color( 37,  99, 235);
    private static final Color[] PALETA    = {
            C_AZUL, C_APROBADO, new Color(234,88,12), new Color(124,58,237),
            new Color(6,148,162), new Color(219,39,119), new Color(5,150,105), new Color(217,119,6)
    };
    // Neutros
    private static final Color INK    = new Color( 15,  23,  42);  // slate-900
    private static final Color INK2   = new Color( 71,  85, 105);  // slate-500
    private static final Color RULE   = new Color(226, 232, 240);  // slate-200
    private static final Color BGPAGE = new Color(248, 250, 252);  // slate-50

    // =========================================================================
    public byte[] generar(ReportePrepostulacionConfigDTO cfg, Usuario usuario) {
        DatosReporte d = cargarDatos(cfg);
        return "EXCEL".equalsIgnoreCase(cfg.getFormato()) ? generarExcel(cfg, d, usuario) : generarPdf(cfg, d, usuario);
    }
    public byte[] generar(ReportePrepostulacionConfigDTO cfg) { return generar(cfg, null); }

    public String nombreArchivo(ReportePrepostulacionConfigDTO cfg) {
        return "reporte_prepostulaciones_" + LocalDateTime.now().format(FMT_NOMBRE)
                + ("EXCEL".equalsIgnoreCase(cfg.getFormato()) ? ".xlsx" : ".pdf");
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================
    private DatosReporte cargarDatos(ReportePrepostulacionConfigDTO cfg) {
        DatosReporte d = new DatosReporte();
        d.institucion   = instRepo.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActivo())).findFirst().orElse(null);
        d.convocatorias = convRepo.findAllByOrderByFechaPublicacionDesc();
        Date desde = parseDate(cfg.getDesde()), hasta = parseDate(cfg.getHasta());

        // ── Query A: prepostulaciones ÚNICAS (sin JOIN a convocatoria) ──────
        // Usada para KPIs, gráficos y conteos. Nunca tiene duplicados.
        List<Map<String,Object>> rowsUnicos = jdbc.queryForList(
                buildSqlUnicos(cfg), buildParams(cfg, desde, hasta).toArray());

        d.kpiEstados = calcEstados(rowsUnicos);
        d.kpiConv    = calcConvocatoria(rowsUnicos);
        d.kpiTemp    = calcTemporal(rowsUnicos);
        d.total      = rowsUnicos.size();
        d.aprobadas  = d.kpiEstados.getOrDefault("APROBADO",  0);
        d.rechazadas = d.kpiEstados.getOrDefault("RECHAZADO", 0);
        d.pendientes = d.kpiEstados.getOrDefault("PENDIENTE", 0);

        // ── Query B: con JOIN para el detalle (puede tener múltiples convocatorias por persona) ──
        if (cfg.isIncluirDetalle()) {
            d.rows = jdbc.queryForList(
                    buildSqlDetalle(cfg), buildParams(cfg, desde, hasta).toArray());
        } else {
            d.rows = rowsUnicos;
        }
        return d;
    }

    /**
     * Query A — solo tabla prepostulacion + filtros por convocatoria/solicitud via EXISTS.
     * Nunca produce duplicados. Usada para KPIs y gráficos.
     */
    private String buildSqlUnicos(ReportePrepostulacionConfigDTO c) {
        StringBuilder sb = new StringBuilder(
                "SELECT p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                        " p.correo, p.estado_revision AS \"estadoRevision\"," +
                        " p.fecha_envio AS \"fechaEnvio\", p.fecha_revision AS \"fechaRevision\"," +
                        " p.observaciones_revision AS \"observacionesRevision\"," +
                        " NULL::bigint AS \"idSolicitud\"," +
                        " NULL::bigint AS \"idConvocatoria\", NULL::text AS \"tituloConvocatoria\"" +
                        " FROM prepostulacion p" +
                        " WHERE 1=1");
        if (ok(c.getDesde()))          sb.append(" AND p.fecha_envio >= ?");
        if (ok(c.getHasta()))          sb.append(" AND p.fecha_envio <= ?");
        if (ok(c.getEstadoRevision())) sb.append(" AND UPPER(p.estado_revision) = UPPER(?)");
        // Filtro por convocatoria: usa EXISTS para no duplicar
        if (c.getIdsConvocatoria() != null && !c.getIdsConvocatoria().isEmpty()) {
            sb.append(" AND EXISTS (SELECT 1 FROM prepostulacion_solicitud ps2" +
                            " JOIN convocatoria_solicitud cs2 ON cs2.id_solicitud = ps2.id_solicitud" +
                            " WHERE ps2.id_prepostulacion = p.id_prepostulacion" +
                            " AND cs2.id_convocatoria IN (")
                    .append(c.getIdsConvocatoria().stream().map(x->"?").collect(java.util.stream.Collectors.joining(","))).append("))");
        }
        if (c.getIdsSolicitud() != null && !c.getIdsSolicitud().isEmpty()) {
            sb.append(" AND EXISTS (SELECT 1 FROM prepostulacion_solicitud ps3" +
                            " WHERE ps3.id_prepostulacion = p.id_prepostulacion" +
                            " AND ps3.id_solicitud IN (")
                    .append(c.getIdsSolicitud().stream().map(x->"?").collect(java.util.stream.Collectors.joining(","))).append("))");
        }
        sb.append(" ORDER BY p.fecha_envio DESC");
        return sb.toString();
    }

    /**
     * Query B — con JOINs para mostrar convocatoria en el detalle.
     * Puede tener múltiples filas por persona (una por convocatoria).
     * Solo se usa para la tabla de detalle del PDF/Excel.
     */
    private String buildSqlDetalle(ReportePrepostulacionConfigDTO c) {
        StringBuilder sb = new StringBuilder(
                "SELECT p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                        " p.correo, p.estado_revision AS \"estadoRevision\"," +
                        " p.fecha_envio AS \"fechaEnvio\", p.fecha_revision AS \"fechaRevision\"," +
                        " p.observaciones_revision AS \"observacionesRevision\"," +
                        " ps.id_solicitud AS \"idSolicitud\"," +
                        " cv.id_convocatoria AS \"idConvocatoria\", cv.titulo AS \"tituloConvocatoria\"" +
                        " FROM prepostulacion p" +
                        " LEFT JOIN prepostulacion_solicitud ps ON ps.id_prepostulacion = p.id_prepostulacion" +
                        " LEFT JOIN convocatoria_solicitud cs ON cs.id_solicitud = ps.id_solicitud" +
                        " LEFT JOIN convocatoria cv ON cv.id_convocatoria = cs.id_convocatoria" +
                        " WHERE 1=1");
        if (ok(c.getDesde()))          sb.append(" AND p.fecha_envio >= ?");
        if (ok(c.getHasta()))          sb.append(" AND p.fecha_envio <= ?");
        if (ok(c.getEstadoRevision())) sb.append(" AND UPPER(p.estado_revision) = UPPER(?)");
        if (c.getIdsConvocatoria() != null && !c.getIdsConvocatoria().isEmpty())
            sb.append(" AND cv.id_convocatoria IN (")
                    .append(c.getIdsConvocatoria().stream().map(x->"?").collect(java.util.stream.Collectors.joining(","))).append(")");
        if (c.getIdsSolicitud() != null && !c.getIdsSolicitud().isEmpty())
            sb.append(" AND ps.id_solicitud IN (")
                    .append(c.getIdsSolicitud().stream().map(x->"?").collect(java.util.stream.Collectors.joining(","))).append(")");
        sb.append(" ORDER BY p.fecha_envio DESC, cv.titulo");
        if (c.getLimite() != null && c.getLimite() > 0 && c.getLimite() < 9999)
            sb.append(" LIMIT ").append(c.getLimite() * 4); // margen para múltiples convocatorias
        return sb.toString();
    }

    private List<Object> buildParams(ReportePrepostulacionConfigDTO c, Date desde, Date hasta) {
        List<Object> p = new ArrayList<>();
        if (desde != null) p.add(desde);
        if (hasta != null) p.add(hasta);
        if (ok(c.getEstadoRevision())) p.add(c.getEstadoRevision());
        if (c.getIdsConvocatoria() != null) p.addAll(c.getIdsConvocatoria());
        if (c.getIdsSolicitud()    != null) p.addAll(c.getIdsSolicitud());
        return p;
    }

    private Map<String,Integer> calcEstados(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        m.put("PENDIENTE",0); m.put("APROBADO",0); m.put("RECHAZADO",0);
        for (var r : rows) m.merge(str(r.get("estadoRevision")).toUpperCase(), 1, Integer::sum);
        return m;
    }
    private Map<String,Integer> calcConvocatoria(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) { String t = str(r.get("tituloConvocatoria")); if (!"—".equals(t)) m.merge(t,1,Integer::sum); }
        return m.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }
    private Map<String,Integer> calcTemporal(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) { Object f = r.get("fechaEnvio"); if (f!=null) m.merge(f.toString().substring(0,7),1,Integer::sum); }
        return new TreeMap<>(m);
    }

    // =========================================================================
    // PDF  —  diseño minimalista
    // =========================================================================
    private byte[] generarPdf(ReportePrepostulacionConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean horiz = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            com.lowagie.text.Rectangle ps = horiz ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(ps, 44, 44, 52, 46);
            PdfWriter w  = PdfWriter.getInstance(doc, baos);
            Color cp     = parseColor(cfg.getColorPrimario());

            // ── Metadatos PDF ─────────────────────────────────────────────
            doc.addTitle(ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones");
            if (d.institucion != null && d.institucion.getNombre() != null)
                doc.addCreator(d.institucion.getNombre());
            if (usuario != null) {
                doc.addAuthor(usuario.getUsuarioApp());
                doc.addSubject("Generado por: " + usuario.getUsuarioApp() + " <" + usuario.getCorreo() + "> — " + LocalDateTime.now().format(FMT_TS));
                doc.addKeywords(usuario.getCorreo());
            }

            // ── Pie de página ─────────────────────────────────────────────
            w.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter wr, Document dc) {
                    try {
                        PdfContentByte cb = wr.getDirectContent();
                        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                        cb.setColorStroke(RULE); cb.setLineWidth(0.5f);
                        cb.moveTo(dc.left(), dc.bottom()-5); cb.lineTo(dc.right(), dc.bottom()-5); cb.stroke();
                        cb.beginText(); cb.setFontAndSize(bf, 7f); cb.setColorFill(INK2);
                        String izq = d.institucion != null && d.institucion.getNombre() != null ? d.institucion.getNombre() : "";
                        String rev = usuario != null ? usuario.getUsuarioApp() + "  ·  " : "";
                        String der = rev + (cfg.isMostrarFechaGeneracion() ? LocalDateTime.now().format(FMT_TS) + "  ·  " : "")
                                + (cfg.isMostrarNumeroPagina() ? "Pág. " + wr.getPageNumber() : "");
                        cb.showTextAligned(Element.ALIGN_LEFT,  izq, dc.left(),  dc.bottom()-15, 0);
                        cb.showTextAligned(Element.ALIGN_RIGHT, der, dc.right(), dc.bottom()-15, 0);
                        cb.endText();
                    } catch (Exception ignored) {}
                }
            });

            doc.open();
            float pw = doc.getPageSize().getWidth();
            float ph = doc.getPageSize().getHeight();
            float ml = doc.leftMargin(), mr = doc.rightMargin();
            float cw = pw - ml - mr;

            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfR = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

            // =================================================================
            // PORTADA  —  minimalista: solo tipografía + línea + datos
            // =================================================================
            if (cfg.isIncluirPortada()) {
                PdfContentByte cv = w.getDirectContent();

                // Franja de color en el borde izquierdo (acento vertical)
                cv.setColorFill(cp);
                cv.rectangle(0, 0, 6, ph);
                cv.fill();

                // Bloque de texto posicionado directamente con canvas
                float startY = ph - 80;

                // Etiqueta pequeña
                cv.beginText();
                cv.setFontAndSize(bfR, 8f);
                cv.setColorFill(INK2);
                cv.showTextAligned(Element.ALIGN_LEFT,
                        d.institucion != null && d.institucion.getNombre() != null ? d.institucion.getNombre().toUpperCase() : "INSTITUCIÓN",
                        ml, startY, 0);
                cv.endText();

                // Línea horizontal delgada
                cv.setColorFill(cp);
                cv.rectangle(ml, startY - 8, cw, 2f);
                cv.fill();

                // Título grande
                cv.beginText();
                cv.setFontAndSize(bfB, 28f);
                cv.setColorFill(INK);
                String titulo = ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones";
                cv.showTextAligned(Element.ALIGN_LEFT, titulo, ml, startY - 44, 0);
                cv.endText();

                // Subtítulo
                if (ok(cfg.getSubtitulo())) {
                    cv.beginText();
                    cv.setFontAndSize(bfR, 11f);
                    cv.setColorFill(INK2);
                    cv.showTextAligned(Element.ALIGN_LEFT, cfg.getSubtitulo(), ml, startY - 64, 0);
                    cv.endText();
                }

                // Espacio para que el doc.add() empiece debajo del canvas
                doc.add(new Paragraph("\n\n\n\n\n\n\n\n\n\n"));

                // ── Línea de metadatos ──────────────────────────────────
                PdfPTable tmeta = new PdfPTable(3);
                tmeta.setWidthPercentage(100);
                tmeta.setSpacingBefore(0);
                tmeta.setSpacingAfter(28);

                Font fML = FontFactory.getFont(FontFactory.HELVETICA,      7.5f, INK2);
                Font fMV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f,  INK);

                String periodo = (ok(cfg.getDesde()) ? cfg.getDesde() : "inicio") + " → " + (ok(cfg.getHasta()) ? cfg.getHasta() : "hoy");
                String autor   = usuario != null ? usuario.getUsuarioApp() : "—";

                metaCell(tmeta, "PERÍODO",              periodo,                                  fML, fMV, cp);
                metaCell(tmeta, "GENERADO POR",         autor,                                    fML, fMV, cp);
                metaCell(tmeta, "FECHA",                LocalDateTime.now().format(FMT_TS),       fML, fMV, cp);
                doc.add(tmeta);

                // ── KPIs ─────────────────────────────────────────────
                PdfPTable tkpi = new PdfPTable(4);
                tkpi.setWidthPercentage(100); tkpi.setSpacingAfter(28);
                kpi(tkpi, "TOTAL",      d.total,      cp,           new Color(240,253,244));
                kpi(tkpi, "APROBADAS",  d.aprobadas,  C_APROBADO,   new Color(240,253,244));
                kpi(tkpi, "RECHAZADAS", d.rechazadas, C_RECHAZADO,  new Color(254,242,242));
                kpi(tkpi, "PENDIENTES", d.pendientes, C_PENDIENTE,  new Color(254,252,232));
                doc.add(tkpi);

                // ── Pastel + leyenda ──────────────────────────────────
                if (d.total > 0 && cfg.isIncluirGraficoEstados()) {
                    PdfPTable tp = pastelTable(d, 160);
                    tp.setSpacingAfter(14);
                    doc.add(tp);
                }

                doc.newPage();
            }

            // =================================================================
            // PÁGINAS DE CONTENIDO
            // =================================================================
            if (cfg.isIncluirKpis()) {
                sec(doc, "Resumen estadístico", cp);
                PdfPTable tkpi = new PdfPTable(4);
                tkpi.setWidthPercentage(100); tkpi.setSpacingAfter(20);
                kpi(tkpi, "TOTAL",      d.total,      cp,           new Color(240,253,244));
                kpi(tkpi, "APROBADAS",  d.aprobadas,  C_APROBADO,   new Color(240,253,244));
                kpi(tkpi, "RECHAZADAS", d.rechazadas, C_RECHAZADO,  new Color(254,242,242));
                kpi(tkpi, "PENDIENTES", d.pendientes, C_PENDIENTE,  new Color(254,252,232));
                doc.add(tkpi);
            }

            if (cfg.isIncluirGraficoEstados() && d.total > 0) {
                sec(doc, "Distribución por estado", cp);
                PdfPTable tp = pastelTable(d, 185);
                tp.setSpacingAfter(20); doc.add(tp);
            }

            if (cfg.isIncluirGraficoConvocatoria() && !d.kpiConv.isEmpty()) {
                sec(doc, "Prepostulaciones por convocatoria", cp);
                int n = Math.min(d.kpiConv.size(), 8);
                float barH = 26f, gap = 7f;
                PdfPTable tb = new PdfPTable(1); tb.setWidthPercentage(100); tb.setSpacingAfter(20);
                PdfPCell cb2 = new PdfPCell();
                cb2.setFixedHeight(n * (barH + gap) + 20); cb2.setBorder(0);
                cb2.setCellEvent((cell, pos, cvs) -> {
                    PdfContentByte cb3 = cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb3.setColorFill(BGPAGE); cb3.rectangle(pos.getLeft(), pos.getBottom(), pos.getWidth(), pos.getHeight()); cb3.fill();
                    drawBarsH(cb3, pos, d.kpiConv, barH, gap, bfB, bfR);
                });
                tb.addCell(cb2); doc.add(tb);
            }

            if (cfg.isIncluirGraficoTemporal() && !d.kpiTemp.isEmpty()) {
                sec(doc, "Evolución temporal", cp);
                PdfPTable tb = new PdfPTable(1); tb.setWidthPercentage(100); tb.setSpacingAfter(20);
                PdfPCell cl = new PdfPCell();
                cl.setFixedHeight(185); cl.setBorder(0);
                cl.setCellEvent((cell, pos, cvs) -> {
                    PdfContentByte cb3 = cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb3.setColorFill(BGPAGE); cb3.rectangle(pos.getLeft(), pos.getBottom(), pos.getWidth(), pos.getHeight()); cb3.fill();
                    drawLine(cb3, pos, d.kpiTemp, cp, bfB, bfR);
                });
                tb.addCell(cl); doc.add(tb);
            }

            if (cfg.isIncluirDetalle() && !d.rows.isEmpty()) {
                doc.newPage();
                sec(doc, "Detalle de prepostulaciones  (" + d.total + " postulantes)", cp);
                detalle(doc, d.rows, cp);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("PDF error: " + e.getMessage(), e); }
    }

    // ─── Portada: celda de metadato ──────────────────────────────────────────
    private void metaCell(PdfPTable t, String label, String valor, Font fL, Font fV, Color cp) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM); c.setBorderColorBottom(cp); c.setBorderWidthBottom(1.5f);
        c.setPadding(8); c.setPaddingBottom(10);
        c.addElement(new Paragraph(label, fL));
        c.addElement(new Paragraph(valor, fV));
        t.addCell(c);
    }

    // ─── Tabla pastel + leyenda ───────────────────────────────────────────────
    private PdfPTable pastelTable(DatosReporte d, float height) {
        PdfPTable tp = new PdfPTable(2);
        tp.setWidthPercentage(84); tp.setWidths(new float[]{1, 1.2f});
        tp.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cpie = new PdfPCell();
        cpie.setFixedHeight(height); cpie.setBorder(0);
        cpie.setCellEvent((cell, pos, cvs) -> {
            PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
            float cx = pos.getLeft() + pos.getWidth() / 2;
            float cy = pos.getBottom() + pos.getHeight() / 2;
            float r  = Math.min(pos.getWidth(), pos.getHeight()) / 2 - 10;
            drawPastel(cb, cx, cy, r, d);
        });
        tp.addCell(cpie);

        PdfPCell cley = new PdfPCell();
        cley.setBorder(0); cley.setPaddingLeft(18);
        cley.setVerticalAlignment(Element.ALIGN_MIDDLE);
        buildLeyenda(cley, d);
        tp.addCell(cley);
        return tp;
    }

    // ─── Gráfico pastel donut ─────────────────────────────────────────────────
    private void drawPastel(PdfContentByte cb, float cx, float cy, float r, DatosReporte d) {
        Color[] cols  = {C_PENDIENTE, C_APROBADO, C_RECHAZADO};
        int[]   vals  = {d.kpiEstados.getOrDefault("PENDIENTE",0), d.kpiEstados.getOrDefault("APROBADO",0), d.kpiEstados.getOrDefault("RECHAZADO",0)};
        int total = d.total == 0 ? 1 : d.total;
        float iR  = r * 0.42f;
        int   N   = 80;
        double startDeg = 90.0;

        for (int i = 0; i < 3; i++) {
            if (vals[i] == 0) continue;
            double sweep = 360.0 * vals[i] / total;
            double endDeg = startDeg - sweep;
            cb.setColorFill(cols[i]); cb.setColorStroke(Color.WHITE); cb.setLineWidth(1.8f);
            cb.moveTo(cx + (float)(r  * Math.cos(Math.toRadians(startDeg))), cy + (float)(r  * Math.sin(Math.toRadians(startDeg))));
            for (int s=0; s<=N; s++) { double a=Math.toRadians(startDeg-sweep*s/N); cb.lineTo(cx+(float)(r*Math.cos(a)), cy+(float)(r*Math.sin(a))); }
            cb.lineTo(cx+(float)(iR*Math.cos(Math.toRadians(endDeg))), cy+(float)(iR*Math.sin(Math.toRadians(endDeg))));
            for (int s=0; s<=N; s++) { double a=Math.toRadians(endDeg+sweep*s/N); cb.lineTo(cx+(float)(iR*Math.cos(a)), cy+(float)(iR*Math.sin(a))); }
            cb.closePath(); cb.fillStroke();
            if (sweep > 14) {
                double mA = Math.toRadians(startDeg - sweep/2);
                float tx = cx+(float)((r*0.68f)*Math.cos(mA)), ty = cy+(float)((r*0.68f)*Math.sin(mA));
                try {
                    BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
                    cb.beginText(); cb.setFontAndSize(bf, 9f); cb.setColorFill(Color.WHITE);
                    cb.showTextAligned(Element.ALIGN_CENTER, String.format("%.0f%%", 100.0*vals[i]/total), tx, ty-3, 0);
                    cb.endText();
                } catch (Exception ignored) {}
            }
            startDeg = endDeg;
        }
        // Círculo interior
        cb.setColorFill(Color.WHITE); cb.setColorStroke(RULE); cb.setLineWidth(0.5f);
        cb.arc(cx-iR, cy-iR, cx+iR, cy+iR, 0, 360); cb.fillStroke();
        try {
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfR = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
            cb.beginText();
            cb.setFontAndSize(bfB, 14f); cb.setColorFill(INK);
            cb.showTextAligned(Element.ALIGN_CENTER, String.valueOf(d.total), cx, cy+3, 0);
            cb.setFontAndSize(bfR, 7f); cb.setColorFill(INK2);
            cb.showTextAligned(Element.ALIGN_CENTER, "total", cx, cy-9, 0);
            cb.endText();
        } catch (Exception ignored) {}
    }

    private void buildLeyenda(PdfPCell cell, DatosReporte d) {
        int total = d.total == 0 ? 1 : d.total;
        Color[] cols  = {C_PENDIENTE, C_APROBADO, C_RECHAZADO};
        String[] keys = {"PENDIENTE", "APROBADO",  "RECHAZADO"};
        Font fT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f,   INK);
        Font fS = FontFactory.getFont(FontFactory.HELVETICA,      7.5f, INK2);
        cell.addElement(new Paragraph("Estados", fT));
        cell.addElement(new Paragraph(" "));
        for (int i=0; i<3; i++) {
            int cant = d.kpiEstados.getOrDefault(keys[i], 0);
            double pct = cant * 100.0 / total;
            PdfPTable row = new PdfPTable(new float[]{5,60,35});
            row.setWidthPercentage(100); row.setSpacingBefore(7);
            PdfPCell dot = new PdfPCell(new Phrase(" "));
            dot.setBackgroundColor(cols[i]); dot.setBorder(0); dot.setFixedHeight(12); row.addCell(dot);
            PdfPCell lbl = new PdfPCell(); lbl.setBorder(0); lbl.setPaddingLeft(7);
            lbl.addElement(new Paragraph(keys[i], fS));
            lbl.addElement(new Paragraph(cant + " registro" + (cant!=1?"s":""), fS));
            row.addCell(lbl);
            Font fp = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, cols[i]);
            PdfPCell pc = new PdfPCell(new Phrase(String.format("%.1f%%", pct), fp));
            pc.setBorder(0); pc.setVerticalAlignment(Element.ALIGN_MIDDLE); row.addCell(pc);
            cell.addElement(row);
        }
    }

    // ─── Barras horizontales ──────────────────────────────────────────────────
    private void drawBarsH(PdfContentByte cb, Rectangle pos, Map<String,Integer> data,
                           float barH, float gap, BaseFont bfB, BaseFont bfR) {
        if (data.isEmpty()) return;
        int maxV = data.values().stream().max(Integer::compareTo).orElse(1);
        float lblW = 170f, barAreaW = pos.getWidth() - lblW - 46f;
        float x0 = pos.getLeft() + 8, y = pos.getTop() - 12;
        int idx = 0;
        try {
            for (var e : data.entrySet()) {
                if (idx >= 8) break;
                Color barC = PALETA[idx % PALETA.length];
                int cant = e.getValue();
                float barW = (float) cant / maxV * barAreaW;
                float yBar = y - barH;

                if (idx % 2 == 1) {
                    cb.setColorFill(new Color(241,245,249));
                    cb.rectangle(pos.getLeft(), yBar - 3, pos.getWidth(), barH + 6); cb.fill();
                }
                cb.beginText(); cb.setFontAndSize(bfR, 8.5f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_LEFT, truncar(e.getKey(), 25), x0, yBar + barH/3f, 0);
                cb.endText();

                if (barW > 0) {
                    cb.setColorFill(barC); cb.rectangle(x0 + lblW, yBar, barW, barH); cb.fill();
                    cb.setColorFill(new Color(255,255,255,40));
                    cb.rectangle(x0 + lblW, yBar + barH*0.6f, barW, barH*0.35f); cb.fill();
                }
                cb.beginText(); cb.setFontAndSize(bfB, 9f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_LEFT, String.valueOf(cant), x0 + lblW + barW + 6, yBar + barH/3f, 0);
                cb.endText();
                y -= (barH + gap); idx++;
            }
        } catch (Exception ignored) {}
    }

    // ─── Línea temporal ───────────────────────────────────────────────────────
    private void drawLine(PdfContentByte cb, Rectangle pos, Map<String,Integer> data,
                          Color cp, BaseFont bfB, BaseFont bfR) {
        if (data.isEmpty()) return;
        List<String>  meses = new ArrayList<>(data.keySet());
        List<Integer> vals  = new ArrayList<>(data.values());
        int n = meses.size(), maxV = vals.stream().max(Integer::compareTo).orElse(1);
        float pL=48, pR=20, pT=16, pB=36;
        float cX=pos.getLeft()+pL, cY=pos.getBottom()+pB, cW=pos.getWidth()-pL-pR, cH=pos.getHeight()-pT-pB;
        try {
            for (int gi=0; gi<=4; gi++) {
                float gy = cY + cH*gi/4f;
                cb.setColorStroke(RULE); cb.setLineWidth(0.4f);
                cb.moveTo(cX, gy); cb.lineTo(cX+cW, gy); cb.stroke();
                cb.beginText(); cb.setFontAndSize(bfR, 7f); cb.setColorFill(INK2);
                cb.showTextAligned(Element.ALIGN_RIGHT, String.valueOf(maxV*gi/4), cX-6, gy-3, 0);
                cb.endText();
            }
            cb.setColorStroke(RULE); cb.setLineWidth(0.8f);
            cb.moveTo(cX, cY); cb.lineTo(cX+cW, cY); cb.stroke();

            float[] px=new float[n], py=new float[n];
            for (int i=0;i<n;i++){
                px[i]=cX+(n==1?cW/2:(float)i/(n-1)*cW);
                py[i]=cY+(float)vals.get(i)/maxV*cH;
            }
            if (n>1) {
                cb.setColorFill(new Color(cp.getRed(),cp.getGreen(),cp.getBlue(),22));
                cb.moveTo(px[0],cY);
                for (int i=0;i<n;i++) cb.lineTo(px[i],py[i]);
                cb.lineTo(px[n-1],cY); cb.closePath(); cb.fill();
            }
            cb.setColorStroke(cp); cb.setLineWidth(2.2f);
            cb.moveTo(px[0],py[0]);
            for (int i=1;i<n;i++) cb.lineTo(px[i],py[i]);
            cb.stroke();

            for (int i=0;i<n;i++){
                cb.setColorFill(cp); cb.circle(px[i],py[i],4.5f); cb.fill();
                cb.setColorFill(Color.WHITE); cb.circle(px[i],py[i],2.5f); cb.fill();
                cb.beginText(); cb.setFontAndSize(bfB, 8f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_CENTER, String.valueOf(vals.get(i)), px[i], py[i]+9, 0);
                cb.endText();
                cb.beginText(); cb.setFontAndSize(bfR, 7.5f); cb.setColorFill(INK2);
                String mes = meses.get(i).length()>7?meses.get(i).substring(2):meses.get(i);
                cb.showTextAligned(Element.ALIGN_CENTER, mes, px[i], cY-13, 0);
                cb.endText();
            }
        } catch (Exception ignored) {}
    }

    // ─── KPI card ─────────────────────────────────────────────────────────────
    private void kpi(PdfPTable t, String label, int valor, Color color, Color bg) {
        Font fL = FontFactory.getFont(FontFactory.HELVETICA,      7f,  INK2);
        Font fV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, color);
        PdfPTable inner = new PdfPTable(new float[]{4,96}); inner.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell(new Phrase(" ")); bar.setBackgroundColor(color); bar.setBorder(0); bar.setPadding(0);
        inner.addCell(bar);
        PdfPCell con = new PdfPCell(); con.setBorder(0); con.setPadding(10);
        con.addElement(new Paragraph(label, fL)); con.addElement(new Paragraph(String.valueOf(valor), fV));
        inner.addCell(con);
        PdfPCell card = new PdfPCell(); card.addElement(inner);
        card.setBackgroundColor(bg); card.setBorderColor(RULE); card.setBorderWidth(0.6f); card.setPadding(0);
        t.addCell(card);
    }

    // ─── Sección ─────────────────────────────────────────────────────────────
    private void sec(Document doc, String titulo, Color cp) throws DocumentException {
        doc.add(new Paragraph("\n"));
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, INK);
        Paragraph p = new Paragraph(titulo, f); p.setSpacingAfter(6); doc.add(p);
        doc.add(new Chunk(new LineSeparator(1.5f, 100, cp, Element.ALIGN_CENTER, 0)));
        doc.add(new Paragraph("\n"));
    }

    // ─── Tabla detalle ────────────────────────────────────────────────────────
    private void detalle(Document doc, List<Map<String,Object>> rows, Color cp) throws DocumentException {
        String[] hdrs={"ID","Nombres","Apellidos","Cédula","Estado","Fecha","Convocatoria"};
        float[]  ws  ={5,17,17,13,11,12,25};
        PdfPTable t = new PdfPTable(hdrs.length);
        t.setWidthPercentage(100); t.setWidths(ws); t.setSpacingBefore(4);
        Font fH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
        for (String h : hdrs) {
            PdfPCell c = new PdfPCell(new Phrase(h, fH));
            c.setBackgroundColor(cp); c.setPadding(6); c.setBorderColor(cp); t.addCell(c);
        }
        Font fR  = FontFactory.getFont(FontFactory.HELVETICA,      7.5f, INK);
        Font fRB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, INK);
        boolean alt = false;
        for (var row : rows) {
            Color bg = alt ? new Color(248,250,252) : Color.WHITE; alt = !alt;
            dc(t, str(row.get("id_prepostulacion")), fRB, bg);
            dc(t, str(row.get("nombres")),           fR,  bg);
            dc(t, str(row.get("apellidos")),         fR,  bg);
            dc(t, str(row.get("identificacion")),    fR,  bg);
            String est = str(row.get("estadoRevision")).toUpperCase();
            Color bgE=bg; Font fE=fR;
            if ("APROBADO".equals(est))  { bgE=new Color(220,252,231); fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(21,128,61)); }
            if ("RECHAZADO".equals(est)) { bgE=new Color(254,226,226); fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(185,28,28)); }
            if ("PENDIENTE".equals(est)) { bgE=new Color(254,249,195); fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(133,77,14)); }
            dc(t, est, fE, bgE);
            dc(t, formatFecha(row.get("fechaEnvio")),             fR, bg);
            dc(t, truncar(str(row.get("tituloConvocatoria")),28), fR, bg);
        }
        doc.add(t);
    }

    private void dc(PdfPTable t, String v, Font f, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(v!=null?v:"—", f));
        c.setPadding(5); c.setBackgroundColor(bg); c.setBorderColor(RULE); c.setBorderWidth(0.5f);
        t.addCell(c);
    }

    // =========================================================================
    // EXCEL
    // =========================================================================
    private byte[] generarExcel(ReportePrepostulacionConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Color cp = parseColor(cfg.getColorPrimario());
            XlsEst ex = new XlsEst(wb, cp);
            xlsResumen(wb.createSheet("Resumen"), cfg, d, ex, wb, cp, usuario);
            if (cfg.isIncluirGraficoEstados())
                xlsEstados(wb.createSheet("Por Estado"), d, ex, wb);
            if (cfg.isIncluirGraficoConvocatoria() && !d.kpiConv.isEmpty())
                xlsConv(wb.createSheet("Por Convocatoria"), d, ex, wb);
            if (cfg.isIncluirGraficoTemporal() && !d.kpiTemp.isEmpty())
                xlsTemp(wb.createSheet("Evolucion Temporal"), d, ex, wb);
            if (cfg.isIncluirDetalle() && !d.rows.isEmpty())
                xlsDet(wb.createSheet("Detalle"), d.rows, cfg, ex, wb);
            wb.write(baos); return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Excel error", e); }
    }

    private void xlsResumen(XSSFSheet sh, ReportePrepostulacionConfigDTO cfg, DatosReporte d,
                            XlsEst ex, XSSFWorkbook wb, Color cp, Usuario usuario) {
        int f=0;
        Row rT=sh.createRow(f++); rT.setHeightInPoints(36);
        Cell cT=rT.createCell(0); cT.setCellValue(ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Prepostulaciones"); cT.setCellStyle(ex.titulo);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,5));
        if (usuario!=null){ Row r=sh.createRow(f++); Cell c=r.createCell(0); c.setCellValue("Generado por: "+usuario.getUsuarioApp()+" <"+usuario.getCorreo()+">"); c.setCellStyle(ex.sub); sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5)); }
        Row rP=sh.createRow(f++); Cell cP=rP.createCell(0);
        cP.setCellValue("Período: "+(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy")+"   |   "+LocalDateTime.now().format(FMT_TS));
        cP.setCellStyle(ex.sub); sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5)); f++;
        String[] kL={"Total","Aprobadas","Rechazadas","Pendientes"};
        int[]    kV={d.total,d.aprobadas,d.rechazadas,d.pendientes};
        Color[]  kC={cp,C_APROBADO,C_RECHAZADO,C_PENDIENTE};
        Row rH=sh.createRow(f++);
        for(int i=0;i<4;i++){Cell c=rH.createCell(i);c.setCellValue(kL[i]);c.setCellStyle(xlsCH(wb,kC[i]));}
        Row rV=sh.createRow(f++); rV.setHeightInPoints(30);
        for(int i=0;i<4;i++){Cell c=rV.createCell(i);c.setCellValue(kV[i]);c.setCellStyle(xlsKN(wb,kC[i]));}
        f+=2;
        Row rHE=sh.createRow(f++); xlsHR(rHE,new String[]{"Estado","Cantidad","% del Total","Barra"},ex); sh.addMergedRegion(new CellRangeAddress(f-1,f-1,3,5));
        int tot=d.total==0?1:d.total; Color[] ec={C_PENDIENTE,C_APROBADO,C_RECHAZADO}; String[] ek={"PENDIENTE","APROBADO","RECHAZADO"};
        for(int i=0;i<3;i++){
            int v=d.kpiEstados.getOrDefault(ek[i],0); double pct=v*100.0/tot;
            Row r=sh.createRow(f++); r.createCell(0).setCellValue(ek[i]); r.createCell(1).setCellValue(v); r.createCell(2).setCellValue(String.format("%.1f%%",pct));
            int bars=(int)(pct/5); Cell cB=r.createCell(3); cB.setCellValue("█".repeat(Math.max(bars,0))+"░".repeat(Math.max(20-bars,0))); cB.setCellStyle(xlsCT(wb,ec[i]));
            sh.addMergedRegion(new CellRangeAddress(f-1,f-1,3,5));
        }
        sh.setColumnWidth(0,7000);sh.setColumnWidth(1,4000);sh.setColumnWidth(2,4000);sh.setColumnWidth(3,9000);sh.setColumnWidth(4,3000);sh.setColumnWidth(5,3000);
    }

    private void xlsEstados(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb) {
        int f=0; Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0); cT.setCellValue("Por Estado"); cT.setCellStyle(ex.titulo); sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++); xlsHR(hdr,new String[]{"Estado","Cantidad","Porcentaje","Proporción","Descripción"},ex);
        int tot=d.total==0?1:d.total; Color[] cols={C_PENDIENTE,C_APROBADO,C_RECHAZADO}; String[] keys={"PENDIENTE","APROBADO","RECHAZADO"}; String[] descs={"En espera","Aprobados","Rechazados"};
        for(int i=0;i<3;i++){
            int v=d.kpiEstados.getOrDefault(keys[i],0); double pct=v*100.0/tot; Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(keys[i]); r.createCell(1).setCellValue(v); r.createCell(2).setCellValue(String.format("%.2f%%",pct));
            int bars=(int)(pct/5); Cell cB=r.createCell(3); cB.setCellValue("█".repeat(Math.max(bars,0))+"░".repeat(Math.max(20-bars,0))); cB.setCellStyle(xlsCT(wb,cols[i]));
            r.createCell(4).setCellValue(descs[i]);
        }
        f++; Row rTot=sh.createRow(f); Cell l=rTot.createCell(0);l.setCellValue("TOTAL");l.setCellStyle(ex.header); Cell v2=rTot.createCell(1);v2.setCellValue(d.total);v2.setCellStyle(ex.header); Cell p2=rTot.createCell(2);p2.setCellValue("100%");p2.setCellStyle(ex.header);
        for(int i=0;i<5;i++) sh.autoSizeColumn(i); sh.setColumnWidth(3,9000);
    }

    private void xlsConv(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb) {
        int f=0; Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0); cT.setCellValue("Por Convocatoria"); cT.setCellStyle(ex.titulo); sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++); xlsHR(hdr,new String[]{"Pos.","Convocatoria","Cantidad","% del Total","Visual"},ex);
        int tot=d.total==0?1:d.total, maxV=d.kpiConv.values().stream().max(Integer::compareTo).orElse(1), pos=1;
        for(var e:d.kpiConv.entrySet()){
            double pct=e.getValue()*100.0/tot; int bars=(int)(e.getValue()*1.0/maxV*20); Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(pos); r.createCell(1).setCellValue(e.getKey()); r.createCell(2).setCellValue(e.getValue()); r.createCell(3).setCellValue(String.format("%.1f%%",pct));
            Cell cB=r.createCell(4); cB.setCellValue("█".repeat(Math.max(bars,0))); cB.setCellStyle(xlsCT(wb,PALETA[(pos-1)%PALETA.length])); pos++;
        }
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),13000));sh.autoSizeColumn(2);sh.autoSizeColumn(3);sh.setColumnWidth(4,8000);
    }

    private void xlsTemp(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb) {
        int f=0; Row rT=sh.createRow(f++); rT.setHeightInPoints(26); Cell cT=rT.createCell(0); cT.setCellValue("Evolución Temporal"); cT.setCellStyle(ex.titulo); sh.addMergedRegion(new CellRangeAddress(0,0,0,4)); f++;
        Row hdr=sh.createRow(f++); xlsHR(hdr,new String[]{"Mes","N°","Tendencia","Δ vs anterior","Acumulado"},ex);
        List<String> ms=new ArrayList<>(d.kpiTemp.keySet()); List<Integer> vs=new ArrayList<>(d.kpiTemp.values());
        int maxV=vs.stream().max(Integer::compareTo).orElse(1), acum=0;
        for(int i=0;i<ms.size();i++){
            int v=vs.get(i); acum+=v; int prev=i>0?vs.get(i-1):0, delta=i>0?v-prev:0; Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(ms.get(i)); r.createCell(1).setCellValue(v);
            int bars=(int)(v*1.0/maxV*20); Cell cB=r.createCell(2); cB.setCellValue("█".repeat(Math.max(bars,0))); cB.setCellStyle(xlsCT(wb,C_AZUL));
            Cell cD=r.createCell(3); if(i==0){cD.setCellValue("—");}else{cD.setCellValue((delta>=0?"▲ +":"▼ ")+delta);cD.setCellStyle(xlsCT(wb,delta>=0?C_APROBADO:C_RECHAZADO));}
            r.createCell(4).setCellValue(acum);
        }
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(2,9000);sh.autoSizeColumn(3);sh.autoSizeColumn(4);
    }

    private void xlsDet(XSSFSheet sh, List<Map<String,Object>> rows, ReportePrepostulacionConfigDTO cfg, XlsEst ex, XSSFWorkbook wb) {
        String[] hs={"ID","Nombres","Apellidos","Identificación","Correo","Estado","Fecha Envío","Fecha Revisión","Convocatoria","ID Solicitud","Observaciones"};
        Row rH=sh.createRow(0); for(int i = 0; i<hs.length; i++){Cell c=rH.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}
        XSSFCellStyle stA=xlsBg(wb,new Color(220,252,231)), stR=xlsBg(wb,new Color(254,226,226)), stP=xlsBg(wb,new Color(254,249,195));
        int f=1;
        for(var row:rows){
            Row r=sh.createRow(f++); int col=0;
            r.createCell(col++).setCellValue(str(row.get("id_prepostulacion"))); r.createCell(col++).setCellValue(str(row.get("nombres"))); r.createCell(col++).setCellValue(str(row.get("apellidos"))); r.createCell(col++).setCellValue(str(row.get("identificacion"))); r.createCell(col++).setCellValue(str(row.get("correo")));
            Cell cE=r.createCell(col++); String est=str(row.get("estadoRevision")).toUpperCase(); cE.setCellValue(est);
            if("APROBADO".equals(est))cE.setCellStyle(stA); else if("RECHAZADO".equals(est))cE.setCellStyle(stR); else if("PENDIENTE".equals(est))cE.setCellStyle(stP);
            r.createCell(col++).setCellValue(formatFecha(row.get("fechaEnvio"))); r.createCell(col++).setCellValue(formatFecha(row.get("fechaRevision"))); r.createCell(col++).setCellValue(str(row.get("tituloConvocatoria"))); r.createCell(col++).setCellValue(str(row.get("idSolicitud"))); r.createCell(col).setCellValue(str(row.get("observacionesRevision")));
        }
        for(int i=0;i<hs.length;i++) sh.autoSizeColumn(i);
        sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),7000));sh.setColumnWidth(2,Math.max(sh.getColumnWidth(2),7000));sh.setColumnWidth(8,Math.max(sh.getColumnWidth(8),11000));sh.setColumnWidth(10,Math.max(sh.getColumnWidth(10),11000));
        if(cfg.isExcelCongelarEncabezado()) sh.createFreezePane(0,1);
        if(cfg.isExcelFiltrosAutomaticos()) sh.setAutoFilter(new CellRangeAddress(0,0,0,hs.length-1));
    }

    // ── Excel helpers ─────────────────────────────────────────────────────────
    private void xlsHR(Row r, String[] hs, XlsEst ex){ for(int i=0;i<hs.length;i++){Cell c=r.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);} }
    private XSSFCellStyle xlsCH(XSSFWorkbook wb, Color color){ XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont(); f.setBold(true);f.setFontHeightInPoints((short)9);f.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));s.setFont(f);s.setFillForegroundColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);s.setAlignment(HorizontalAlignment.CENTER);return s; }
    private XSSFCellStyle xlsKN(XSSFWorkbook wb, Color color){ XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont(); f.setBold(true);f.setFontHeightInPoints((short)16);f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);s.setAlignment(HorizontalAlignment.CENTER);s.setVerticalAlignment(VerticalAlignment.CENTER);return s; }
    private XSSFCellStyle xlsCT(XSSFWorkbook wb, Color color){ XSSFCellStyle s=wb.createCellStyle(); XSSFFont f=wb.createFont(); f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);return s; }
    private XSSFCellStyle xlsBg(XSSFWorkbook wb, Color bg){ XSSFCellStyle s=wb.createCellStyle(); s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s; }

    private static class XlsEst {
        final XSSFCellStyle titulo,sub,label,header;
        XlsEst(XSSFWorkbook wb, Color cp){
            XSSFFont fT=wb.createFont();fT.setBold(true);fT.setFontHeightInPoints((short)15);fT.setColor(new XSSFColor(cp,new DefaultIndexedColorMap()));
            XSSFFont fH=wb.createFont();fH.setBold(true);fH.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));
            titulo=mk(wb,fT,null); sub=mk(wb,null,null); label=mk(wb,null,null);
            XSSFCellStyle hs=wb.createCellStyle();hs.setFont(fH);hs.setFillForegroundColor(new XSSFColor(cp,new DefaultIndexedColorMap()));hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);hs.setAlignment(HorizontalAlignment.CENTER);header=hs;
        }
        private static XSSFCellStyle mk(XSSFWorkbook wb, XSSFFont font, Color bg){
            XSSFCellStyle s=wb.createCellStyle(); if(font!=null)s.setFont(font);
            if(bg!=null){s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}
            return s;
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static class DatosReporte {
        Institucion institucion; List<Convocatoria> convocatorias;
        List<Map<String,Object>> rows = new ArrayList<>();
        Map<String,Integer> kpiEstados=new LinkedHashMap<>(), kpiConv=new LinkedHashMap<>(), kpiTemp=new LinkedHashMap<>();
        int total, aprobadas, rechazadas, pendientes;
    }

    private Date parseDate(String s){ if(s==null||s.isBlank())return null; try{return Date.valueOf(LocalDate.parse(s));}catch(Exception e){return null;} }
    private String str(Object o){ return o!=null?o.toString():"—"; }
    private boolean ok(String s){ return s!=null&&!s.isBlank(); }
    private String truncar(String s, int max){ if(s==null||"—".equals(s))return"—"; return s.length()>max?s.substring(0,max)+"…":s; }
    private String formatFecha(Object o){ if(o==null)return"—"; try{return LocalDate.parse(o.toString().substring(0,10)).format(FMT_FECHA);}catch(Exception e){return o.toString();} }
    private Color parseColor(String hex){ if(hex==null||hex.isBlank())return C_APROBADO; try{hex=hex.startsWith("#")?hex.substring(1):hex;return new Color(Integer.parseInt(hex.substring(0,2),16),Integer.parseInt(hex.substring(2,4),16),Integer.parseInt(hex.substring(4,6),16));}catch(Exception e){return C_APROBADO;} }
    private Color darken(Color c, float f){ return new Color(Math.max(0,(int)(c.getRed()*(1-f))),Math.max(0,(int)(c.getGreen()*(1-f))),Math.max(0,(int)(c.getBlue()*(1-f)))); }
}