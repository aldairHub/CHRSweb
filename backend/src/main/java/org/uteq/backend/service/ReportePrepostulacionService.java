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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${mistral.api.key:}")
    private String mistralApiKey;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Color C_APROBADO  = new Color( 22, 163,  74);
    private static final Color C_RECHAZADO = new Color(220,  38,  38);
    private static final Color C_PENDIENTE = new Color(202, 138,   4);
    private static final Color C_AZUL      = new Color( 37,  99, 235);
    private static final Color C_VIOLETA   = new Color(124,  58, 237);
    private static final Color[] PALETA    = {
            C_AZUL, C_APROBADO, new Color(234,88,12), C_VIOLETA,
            new Color(6,148,162), new Color(219,39,119), new Color(5,150,105), new Color(217,119,6)
    };
    private static final Color INK   = new Color( 15,  23,  42);
    private static final Color INK2  = new Color( 71,  85, 105);
    private static final Color RULE  = new Color(226, 232, 240);
    private static final Color BGPAGE= new Color(248, 250, 252);
    private static final float BAR_H  = 26f;
    private static final float BAR_GAP = 8f;

    // =========================================================================
    // ENTRADA
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

        // Query A: una fila por postulante (sin duplicados). Para KPIs y gráficos.
        List<Map<String,Object>> unicos = jdbc.queryForList(sqlUnicos(cfg), params(cfg, desde, hasta).toArray());

        d.kpiEstados = calcEstados(unicos);
        d.kpiTemp    = calcTemporal(unicos);
        // kpiConv necesita JOIN a convocatoria — query separada para no duplicar postulantes
        d.kpiConv    = calcConvocatoriaConJoin(cfg, desde, hasta);
        d.total      = unicos.size();
        d.aprobadas  = d.kpiEstados.getOrDefault("APROBADO",  0);
        d.rechazadas = d.kpiEstados.getOrDefault("RECHAZADO", 0);
        d.pendientes = d.kpiEstados.getOrDefault("PENDIENTE", 0);

        // Indicadores secundarios
        d.tasaAprobacion = d.total == 0 ? 0.0 : (d.aprobadas * 100.0 / d.total);

        // Tiempo promedio de revisión (días, solo los ya revisados)
        List<Long> tiempos = unicos.stream()
                .filter(r -> r.get("fechaRevision") != null && r.get("fechaEnvio") != null)
                .map(r -> { try {
                    LocalDate rev = LocalDate.parse(r.get("fechaRevision").toString().substring(0,10));
                    LocalDate env = LocalDate.parse(r.get("fechaEnvio").toString().substring(0,10));
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(env, rev);
                    return dias >= 0 ? dias : null;
                } catch (Exception e) { return null; } })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        d.tiempoPromedioRevision = tiempos.isEmpty() ? 0.0 : tiempos.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // Promedio de documentos adicionales por postulante
        if (d.total > 0 && !unicos.isEmpty()) {
            try {
                List<Object> ids = unicos.stream()
                        .map(r -> { try { return (Object) Long.parseLong(r.get("id_prepostulacion").toString()); } catch (Exception e) { return null; } })
                        .filter(Objects::nonNull).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String ph = ids.stream().map(x -> "?").collect(Collectors.joining(","));
                    Long total = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM prepostulacion_documentos WHERE id_prepostulacion IN (" + ph + ")",
                            Long.class, ids.toArray());
                    d.promedioDocsAdicionales = total != null ? (double) total / d.total : 0.0;
                }
            } catch (Exception ignored) {}
        }

        // Postulantes que subieron MÁS DE 3 títulos/docs adicionales
        if (d.total > 0 && !unicos.isEmpty()) {
            try {
                List<Object> ids = unicos.stream()
                        .map(r -> { try { return (Object) Long.parseLong(r.get("id_prepostulacion").toString()); } catch (Exception e) { return null; } })
                        .filter(Objects::nonNull).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String ph = ids.stream().map(x -> "?").collect(Collectors.joining(","));
                    Long conMasDe3 = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM (" +
                                    "  SELECT id_prepostulacion FROM prepostulacion_documentos" +
                                    "  WHERE id_prepostulacion IN (" + ph + ")" +
                                    "  GROUP BY id_prepostulacion HAVING COUNT(*) > 3" +
                                    ") sub",
                            Long.class, ids.toArray());
                    d.completitudDocumental = conMasDe3 != null ? (conMasDe3 * 100.0 / d.total) : 0.0;
                    d.postulantesConDocs = conMasDe3 != null ? conMasDe3.intValue() : 0;
                }
            } catch (Exception ignored) {}
        }

        // Análisis IA con Gemini (no bloquea si falla)
        try { d.analisisIA = llamarMistral(d); } catch (Exception ignored) {}

        // Query B: con JOINs para la tabla de detalle (puede tener múltiples filas por postulante)
        if (cfg.isIncluirDetalle()) {
            d.rows = jdbc.queryForList(sqlDetalle(cfg), params(cfg, desde, hasta).toArray());
        } else {
            d.rows = unicos;
        }
        return d;
    }

    // ── Query A: sin duplicados, usa EXISTS para filtros de convocatoria ──────
    private String sqlUnicos(ReportePrepostulacionConfigDTO c) {
        StringBuilder sb = new StringBuilder(
                "SELECT p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                        " p.correo, p.estado_revision AS \"estadoRevision\"," +
                        " p.fecha_envio AS \"fechaEnvio\", p.fecha_revision AS \"fechaRevision\"," +
                        " p.observaciones_revision AS \"observacionesRevision\"" +
                        " FROM prepostulacion p WHERE 1=1");
        if (ok(c.getDesde()))          sb.append(" AND p.fecha_envio >= ?");
        if (ok(c.getHasta()))          sb.append(" AND p.fecha_envio <= ?");
        if (ok(c.getEstadoRevision())) sb.append(" AND UPPER(p.estado_revision) = UPPER(?)");
        if (c.getIdsConvocatoria() != null && !c.getIdsConvocatoria().isEmpty())
            sb.append(" AND EXISTS (SELECT 1 FROM prepostulacion_solicitud ps2" +
                            " JOIN convocatoria_solicitud cs2 ON cs2.id_solicitud = ps2.id_solicitud" +
                            " WHERE ps2.id_prepostulacion = p.id_prepostulacion AND cs2.id_convocatoria IN (")
                    .append(c.getIdsConvocatoria().stream().map(x->"?").collect(Collectors.joining(","))).append("))");
        if (c.getIdsSolicitud() != null && !c.getIdsSolicitud().isEmpty())
            sb.append(" AND EXISTS (SELECT 1 FROM prepostulacion_solicitud ps3" +
                            " WHERE ps3.id_prepostulacion = p.id_prepostulacion AND ps3.id_solicitud IN (")
                    .append(c.getIdsSolicitud().stream().map(x->"?").collect(Collectors.joining(","))).append("))");
        sb.append(" ORDER BY p.fecha_envio DESC");
        return sb.toString();
    }

    // ── Query B: con JOINs para mostrar convocatorias en el detalle ───────────
    private String sqlDetalle(ReportePrepostulacionConfigDTO c) {
        // STRING_AGG agrupa todas las convocatorias en UNA sola fila por postulante
        // → nunca hay duplicados en la tabla de detalle
        StringBuilder sb = new StringBuilder(
                "SELECT p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                        " p.correo, p.estado_revision AS \"estadoRevision\"," +
                        " p.fecha_envio AS \"fechaEnvio\", p.observaciones_revision AS \"observacionesRevision\"," +
                        " STRING_AGG(DISTINCT cv.titulo, \' · \' ORDER BY cv.titulo) AS \"tituloConvocatoria\"" +
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
                    .append(c.getIdsConvocatoria().stream().map(x->"?").collect(Collectors.joining(","))).append(")");
        if (c.getIdsSolicitud() != null && !c.getIdsSolicitud().isEmpty())
            sb.append(" AND ps.id_solicitud IN (")
                    .append(c.getIdsSolicitud().stream().map(x->"?").collect(Collectors.joining(","))).append(")");
        sb.append(" GROUP BY p.id_prepostulacion, p.nombres, p.apellidos, p.identificacion," +
                " p.correo, p.estado_revision, p.fecha_envio, p.observaciones_revision");
        sb.append(" ORDER BY p.fecha_envio DESC");
        return sb.toString();
    }

    private List<Object> params(ReportePrepostulacionConfigDTO c, Date desde, Date hasta) {
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
        // Para los KPIs de convocatoria necesitamos el detalle con JOIN
        // Aquí simplemente agrupamos desde las rows únicas (tienen tituloConvocatoria si se une)
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) {
            String t = r.containsKey("tituloConvocatoria") ? str(r.get("tituloConvocatoria")) : "—";
            if (!"—".equals(t)) m.merge(t, 1, Integer::sum);
        }
        return m.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(a,b)->a, LinkedHashMap::new));
    }
    private Map<String,Integer> calcTemporal(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        for (var r : rows) { Object f = r.get("fechaEnvio"); if (f!=null) m.merge(f.toString().substring(0,7),1,Integer::sum); }
        return new TreeMap<>(m);
    }

    // Cuenta prepostulaciones únicas por convocatoria usando JOIN
    private Map<String,Integer> calcConvocatoriaConJoin(ReportePrepostulacionConfigDTO c, Date desde, Date hasta) {
        try {
            StringBuilder sb = new StringBuilder(
                    "SELECT cv.titulo, COUNT(DISTINCT p.id_prepostulacion) AS cnt" +
                            " FROM prepostulacion p" +
                            " JOIN prepostulacion_solicitud ps ON ps.id_prepostulacion = p.id_prepostulacion" +
                            " JOIN convocatoria_solicitud cs ON cs.id_solicitud = ps.id_solicitud" +
                            " JOIN convocatoria cv ON cv.id_convocatoria = cs.id_convocatoria" +
                            " WHERE cv.titulo IS NOT NULL");
            if (ok(c.getDesde())) sb.append(" AND p.fecha_envio >= ?");
            if (ok(c.getHasta())) sb.append(" AND p.fecha_envio <= ?");
            if (ok(c.getEstadoRevision())) sb.append(" AND UPPER(p.estado_revision) = UPPER(?)");
            if (c.getIdsConvocatoria() != null && !c.getIdsConvocatoria().isEmpty())
                sb.append(" AND cv.id_convocatoria IN (")
                        .append(c.getIdsConvocatoria().stream().map(x->"?").collect(Collectors.joining(","))).append(")");
            sb.append(" GROUP BY cv.titulo ORDER BY cnt DESC");

            List<Object> p = new ArrayList<>();
            if (desde != null) p.add(desde);
            if (hasta != null) p.add(hasta);
            if (ok(c.getEstadoRevision())) p.add(c.getEstadoRevision());
            if (c.getIdsConvocatoria() != null) p.addAll(c.getIdsConvocatoria());

            Map<String,Integer> m = new LinkedHashMap<>();
            jdbc.queryForList(sb.toString(), p.toArray())
                    .forEach(row -> m.put(str(row.get("titulo")), ((Number)row.get("cnt")).intValue()));
            return m;
        } catch (Exception e) { return new LinkedHashMap<>(); }
    }

    // =========================================================================
    // GEMINI IA
    // =========================================================================
    private String llamarMistral(DatosReporte d) {
        if (!ok(mistralApiKey) || d.total == 0) return null;

        String top3 = d.kpiConv.entrySet().stream().limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "Eres un analista de procesos académicos universitarios. Con los siguientes datos de " +
                        "prepostulaciones docentes, redacta un análisis profesional en español de 3 a 4 oraciones. " +
                        "Sin listas, sin saludos, sin subtítulos. Solo texto continuo y directo.\n\n" +
                        "Datos:\n" +
                        "- Total postulantes: %d | Aprobados: %d (%.1f%%) | Rechazados: %d | Pendientes: %d\n" +
                        "- Tiempo promedio de revisión: %.1f días\n" +
                        "- Promedio de documentos adicionales por postulante: %.1f\n" +
                        "- Postulantes que entregaron docs adicionales: %d de %d (%.0f%%)\n" +
                        "- Convocatorias con más postulaciones: %s\n\n" +
                        "Analiza eficiencia del proceso, calidad documental y da máximo una recomendación concreta.",
                d.total, d.aprobadas, d.tasaAprobacion, d.rechazadas, d.pendientes,
                d.tiempoPromedioRevision, d.promedioDocsAdicionales,
                d.postulantesConDocs, d.total, d.completitudDocumental, top3);

        try {
            String bodyJson = "{\"model\":\"mistral-small-latest\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" +
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(prompt) + "}]," +
                    "\"max_tokens\":300,\"temperature\":0.35}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.mistral.ai/v1/chat/completions"))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + mistralApiKey)
                    .timeout(java.time.Duration.ofSeconds(35))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.err.println("[Mistral] Error HTTP " + resp.statusCode() + ": " + resp.body());
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode json =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
            String text = json.path("choices").path(0).path("message").path("content").asText("").trim();
            if (text.isBlank()) {
                System.err.println("[Mistral] Texto vacío en respuesta");
                return null;
            }
            System.out.println("[Mistral] Análisis generado correctamente (" + text.length() + " chars)");
            return text;
        } catch (Exception e) {
            System.err.println("[Mistral] Excepción: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // PDF
    // =========================================================================
    private byte[] generarPdf(ReportePrepostulacionConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean horiz = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            com.lowagie.text.Rectangle pageSize = horiz ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(pageSize, 44, 44, 52, 46);
            PdfWriter w  = PdfWriter.getInstance(doc, baos);
            Color cp     = parseColor(cfg.getColorPrimario());

            // Metadatos PDF
            doc.addTitle(ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones");
            if (d.institucion != null && d.institucion.getNombre() != null)
                doc.addCreator(d.institucion.getNombre());
            if (usuario != null) {
                doc.addAuthor(usuario.getUsuarioApp());
                doc.addSubject("Generado por: " + usuario.getUsuarioApp() + " <" + usuario.getCorreo() + "> — " + LocalDateTime.now().format(FMT_TS));
            }

            // Pie de página
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
            float pw = doc.getPageSize().getWidth(), ph = doc.getPageSize().getHeight();
            float ml = doc.leftMargin(), cw = pw - ml - doc.rightMargin();
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfR = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

            // =================================================================
            // PORTADA
            // =================================================================
            if (cfg.isIncluirPortada()) {
                PdfContentByte cv = w.getDirectContent();

                // Franja lateral izquierda
                cv.setColorFill(cp);
                cv.rectangle(0, 0, 6, ph); cv.fill();

                // Línea de color bajo el título
                float startY = ph - 80;
                cv.beginText();
                cv.setFontAndSize(bfR, 8f); cv.setColorFill(INK2);
                cv.showTextAligned(Element.ALIGN_LEFT,
                        d.institucion != null && d.institucion.getNombre() != null
                                ? d.institucion.getNombre().toUpperCase() : "", ml, startY, 0);
                cv.endText();
                cv.setColorFill(cp);
                cv.rectangle(ml, startY - 8, cw, 2f); cv.fill();
                cv.beginText();
                cv.setFontAndSize(bfB, 26f); cv.setColorFill(INK);
                cv.showTextAligned(Element.ALIGN_LEFT, ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Prepostulaciones", ml, startY - 44, 0);
                if (ok(cfg.getSubtitulo())) {
                    cv.setFontAndSize(bfR, 11f); cv.setColorFill(INK2);
                    cv.showTextAligned(Element.ALIGN_LEFT, cfg.getSubtitulo(), ml, startY - 64, 0);
                }
                cv.endText();

                doc.add(new Paragraph("\n\n\n\n\n\n\n\n\n\n"));

                // Franja de metadatos
                PdfPTable tmeta = new PdfPTable(3); tmeta.setWidthPercentage(100); tmeta.setSpacingAfter(22);
                Font fML = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, INK2);
                Font fMV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, INK);
                metaCell(tmeta, "PERÍODO",
                        (ok(cfg.getDesde()) ? cfg.getDesde() : "inicio") + " → " + (ok(cfg.getHasta()) ? cfg.getHasta() : "hoy"),
                        fML, fMV, cp);
                metaCell(tmeta, "GENERADO POR", usuario != null ? usuario.getUsuarioApp() : "—", fML, fMV, cp);
                metaCell(tmeta, "FECHA", LocalDateTime.now().format(FMT_TS), fML, fMV, cp);
                doc.add(tmeta);

                // KPIs principales
                PdfPTable tkpi = new PdfPTable(4); tkpi.setWidthPercentage(100); tkpi.setSpacingAfter(10);
                kpi(tkpi, "TOTAL",      d.total,      cp,           new Color(240,253,244));
                kpi(tkpi, "APROBADAS",  d.aprobadas,  C_APROBADO,   new Color(240,253,244));
                kpi(tkpi, "RECHAZADAS", d.rechazadas, C_RECHAZADO,  new Color(254,242,242));
                kpi(tkpi, "PENDIENTES", d.pendientes, C_PENDIENTE,  new Color(254,252,232));
                doc.add(tkpi);

                // KPIs secundarios
                PdfPTable tsec = kpiSecundarios(d, cp, bfB, bfR, w);
                tsec.setSpacingAfter(20); doc.add(tsec);

                // Pastel en portada
                if (d.total > 0 && cfg.isIncluirGraficoEstados()) {
                    doc.add(pastelConLeyenda(d, 155, bfB, bfR));
                }

                doc.newPage();
            }

            // =================================================================
            // PÁGINA 2: Solo gráficos nuevos. Sin repetir nada de la portada.
            // =================================================================

            // Prepostulaciones por convocatoria — barras a ancho completo con conteo
            if (!d.kpiConv.isEmpty()) {
                sec(doc, "Prepostulaciones por convocatoria", cp);
                final int nConvBars = Math.min(d.kpiConv.size(), 8);
                float chartHConv = nConvBars * (BAR_H + BAR_GAP) + 24;
                PdfPTable tConv = new PdfPTable(1); tConv.setWidthPercentage(100); tConv.setSpacingAfter(20);
                PdfPCell cConv = new PdfPCell(); cConv.setFixedHeight(chartHConv); cConv.setBorder(0);
                cConv.setCellEvent((cell, pos, cvs) -> {
                    PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb.setColorFill(BGPAGE); cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight()); cb.fill();
                    drawBarsHFullWidth(cb, pos, d.kpiConv, nConvBars, d.total, cp, bfB, bfR);
                });
                tConv.addCell(cConv); doc.add(tConv);
            }

            // Evolución temporal
            if (cfg.isIncluirGraficoTemporal() && !d.kpiTemp.isEmpty()) {
                sec(doc, "Evolución temporal", cp);
                PdfPTable tb = new PdfPTable(1); tb.setWidthPercentage(100); tb.setSpacingAfter(20);
                PdfPCell cl = new PdfPCell(); cl.setFixedHeight(185); cl.setBorder(0);
                cl.setCellEvent((cell, pos, cvs) -> {
                    PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb.setColorFill(BGPAGE); cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight()); cb.fill();
                    drawLine(cb, pos, d.kpiTemp, cp, bfB, bfR);
                });
                tb.addCell(cl); doc.add(tb);
            }

            // Análisis IA
            if (d.analisisIA != null && !d.analisisIA.isBlank()) {
                sec(doc, "Análisis del período", cp);
                PdfPTable tIA = new PdfPTable(1); tIA.setWidthPercentage(100); tIA.setSpacingAfter(14);
                PdfPCell cIA = new PdfPCell();
                cIA.setBorder(Rectangle.LEFT); cIA.setBorderColorLeft(cp); cIA.setBorderWidthLeft(3f);
                cIA.setBackgroundColor(new Color(249,250,251)); cIA.setPadding(16); cIA.setPaddingLeft(18);
                Font fAn = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, INK);
                // Normalizar saltos de línea y limpiar el texto de Gemini
                String textoIA = d.analisisIA.replaceAll("\r\n|\r", "\n").trim();
                // Usar Paragraph con leading generoso para el texto completo
                Paragraph pIA = new Paragraph(textoIA, fAn);
                pIA.setLeading(15f);
                cIA.addElement(pIA);
                tIA.addCell(cIA); doc.add(tIA);
                Font fNota = FontFactory.getFont(FontFactory.HELVETICA, 7f, INK2);
                Paragraph pN = new Paragraph("Análisis generado por Mistral AI. Usar como orientación complementaria.", fNota);
                pN.setAlignment(Element.ALIGN_RIGHT); pN.setSpacingAfter(16); doc.add(pN);
            }

            // Detalle
            if (cfg.isIncluirDetalle() && !d.rows.isEmpty()) {
                doc.newPage();
                sec(doc, "Detalle de prepostulaciones  (" + d.total + " postulantes)", cp);
                detalle(doc, d.rows, cp, bfB, bfR);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("PDF error: " + e.getMessage(), e); }
    }

    // ─── Metadatos portada ────────────────────────────────────────────────────
    private void metaCell(PdfPTable t, String lbl, String val, Font fL, Font fV, Color cp) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM); c.setBorderColorBottom(cp); c.setBorderWidthBottom(1.5f);
        c.setPadding(8); c.setPaddingBottom(10);
        c.addElement(new Paragraph(lbl, fL)); c.addElement(new Paragraph(val, fV));
        t.addCell(c);
    }

    // ─── KPI principal ────────────────────────────────────────────────────────
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

    // ─── KPIs secundarios con mini-gráficos vectoriales ──────────────────────
    private PdfPTable kpiSecundarios(DatosReporte d, Color cp, BaseFont bfB, BaseFont bfR, PdfWriter w) {
        PdfPTable t = new PdfPTable(3); t.setWidthPercentage(100);

        // 1. Tiempo promedio de revisión — mini-gauge
        PdfPCell c1 = new PdfPCell();
        c1.setFixedHeight(82); c1.setBorder(Rectangle.BOX); c1.setBorderColor(RULE); c1.setBorderWidth(0.6f);
        c1.setBackgroundColor(new Color(239,246,255));
        c1.setCellEvent((cell, pos, cvs) -> {
            PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
            drawKpiSecundario(cb, pos, "TIEMPO REVISIÓN",
                    d.tiempoPromedioRevision < 1 ? "< 1 día" : String.format("%.1f días", d.tiempoPromedioRevision),
                    "promedio entre envío y resolución", C_AZUL, bfB, bfR);
            // Mini-gauge semicircular
            drawGauge(cb, pos, d.tiempoPromedioRevision, 14.0, C_AZUL);
        });
        t.addCell(c1);

        // 2. Promedio docs adicionales — mini barras verticales
        PdfPCell c2 = new PdfPCell();
        c2.setFixedHeight(82); c2.setBorder(Rectangle.BOX); c2.setBorderColor(RULE); c2.setBorderWidth(0.6f);
        c2.setBackgroundColor(new Color(245,243,255));
        c2.setCellEvent((cell, pos, cvs) -> {
            PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
            drawKpiSecundario(cb, pos, "DOCS. ADICIONALES",
                    String.format("%.1f por postulante", d.promedioDocsAdicionales),
                    "promedio de archivos extra", C_VIOLETA, bfB, bfR);
            drawMiniBarrasDocs(cb, pos, d.promedioDocsAdicionales, 5.0, C_VIOLETA);
        });
        t.addCell(c2);

        // 3. Completitud documental — mini anillo de progreso
        PdfPCell c3 = new PdfPCell();
        c3.setFixedHeight(82); c3.setBorder(Rectangle.BOX); c3.setBorderColor(RULE); c3.setBorderWidth(0.6f);
        c3.setBackgroundColor(new Color(240,253,244));
        c3.setCellEvent((cell, pos, cvs) -> {
            PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
            drawKpiSecundario(cb, pos, "MÁS DE 3 TÍTULOS",
                    String.format("%.0f%% del total", d.completitudDocumental),
                    d.postulantesConDocs + " de " + d.total + " postulantes", C_APROBADO, bfB, bfR);
            drawAnilloProgreso(cb, pos, d.completitudDocumental / 100.0, C_APROBADO);
        });
        t.addCell(c3);

        return t;
    }

    // ─── Dibuja el texto base del KPI secundario ──────────────────────────────
    private void drawKpiSecundario(PdfContentByte cb, Rectangle pos, String label, String valor, String sub,
                                   Color color, BaseFont bfB, BaseFont bfR) {
        try {
            float x = pos.getLeft() + 10, y = pos.getTop() - 16;
            cb.beginText();
            cb.setFontAndSize(bfR, 6.5f); cb.setColorFill(INK2);
            cb.showTextAligned(Element.ALIGN_LEFT, label, x, y, 0);
            cb.setFontAndSize(bfB, 11f);  cb.setColorFill(color);
            cb.showTextAligned(Element.ALIGN_LEFT, valor, x, y - 16, 0);
            cb.setFontAndSize(bfR, 6.5f); cb.setColorFill(INK2);
            cb.showTextAligned(Element.ALIGN_LEFT, sub, x, y - 28, 0);
            cb.endText();
        } catch (Exception ignored) {}
    }

    // ─── Mini gauge semicircular (para tiempo de revisión) ───────────────────
    private void drawGauge(PdfContentByte cb, Rectangle pos, double valor, double maxVal, Color color) {
        float cx = pos.getRight() - 28, cy = pos.getBottom() + 28, r = 20f;
        double pct = Math.min(valor / maxVal, 1.0);
        int N = 40;

        // Arco de fondo
        cb.setColorStroke(new Color(226,232,240)); cb.setLineWidth(4f); cb.setLineCap(1);
        cb.arc(cx - r, cy - r, cx + r, cy + r, 0, 180); cb.stroke();

        // Arco de valor
        if (pct > 0) {
            cb.setColorStroke(color); cb.setLineWidth(4f);
            cb.arc(cx - r, cy - r, cx + r, cy + r, 0, (float)(180 * pct)); cb.stroke();
        }

        // Punto en el extremo
        double endAngle = Math.PI * pct;
        float px = cx + (float)(r * Math.cos(endAngle)), py = cy + (float)(r * Math.sin(endAngle));
        cb.setColorFill(color); cb.circle(px, py, 3); cb.fill();
    }

    // ─── Mini barras verticales para docs adicionales ─────────────────────────
    private void drawMiniBarrasDocs(PdfContentByte cb, Rectangle pos, double promedio, double maxVal, Color color) {
        int numBarras = 5;
        float bw = 8f, gap = 4f;
        float totalW = numBarras * bw + (numBarras - 1) * gap;
        float startX = pos.getRight() - totalW - 10;
        float baseY  = pos.getBottom() + 10;
        float maxH   = 35f;

        for (int i = 0; i < numBarras; i++) {
            float x = startX + i * (bw + gap);
            // Fondo
            cb.setColorFill(new Color(226,232,240));
            cb.rectangle(x, baseY, bw, maxH); cb.fill();
            // Valor llenado proporcionalmente (i+1 = nivel de docs)
            double nivel = i + 1;
            float h = nivel <= promedio ? maxH : (float)(maxH * Math.max(0, promedio - i));
            if (h > 0) {
                cb.setColorFill(nivel <= promedio ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                cb.rectangle(x, baseY, bw, h); cb.fill();
            }
        }
    }

    // ─── Anillo de progreso para completitud documental ───────────────────────
    private void drawAnilloProgreso(PdfContentByte cb, Rectangle pos, double pct, Color color) {
        float cx = pos.getRight() - 28, cy = pos.getBottom() + 28, r = 18f, iR = 12f;
        int N = 60;

        // Anillo fondo
        cb.setColorFill(new Color(226,232,240));
        for (int s = 0; s <= N; s++) {
            double a = 2 * Math.PI * s / N - Math.PI/2;
            double a2 = 2 * Math.PI * (s+1) / N - Math.PI/2;
            cb.moveTo(cx+(float)(r*Math.cos(a)), cy+(float)(r*Math.sin(a)));
            cb.lineTo(cx+(float)(r*Math.cos(a2)),cy+(float)(r*Math.sin(a2)));
        }
        cb.setColorFill(new Color(226,232,240));
        // Dibujamos como sector completo de fondo
        cb.arc(cx-r, cy-r, cx+r, cy+r, 0, 360); cb.setColorStroke(new Color(226,232,240));
        cb.setLineWidth(7f); cb.stroke();

        // Anillo valor
        if (pct > 0) {
            cb.setColorStroke(color); cb.setLineWidth(7f); cb.setLineCap(1);
            cb.arc(cx-r, cy-r, cx+r, cy+r, 90, (float)(-360*pct)); cb.stroke();
        }
        // Centro blanco
        cb.setColorFill(new Color(240,253,244)); cb.circle(cx,cy,iR); cb.fill();
        // % en centro
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            cb.beginText(); cb.setFontAndSize(bf, 7f); cb.setColorFill(color);
            cb.showTextAligned(Element.ALIGN_CENTER, String.format("%.0f%%", pct*100), cx, cy-2, 0);
            cb.endText();
        } catch (Exception ignored) {}
    }

    // ─── Pastel donut + leyenda ───────────────────────────────────────────────
    private PdfPTable pastelConLeyenda(DatosReporte d, float height, BaseFont bfB, BaseFont bfR) {
        PdfPTable tp = new PdfPTable(2); tp.setWidthPercentage(84);
        tp.setWidths(new float[]{1,1.2f}); tp.setHorizontalAlignment(Element.ALIGN_CENTER);
        tp.setSpacingAfter(14);
        PdfPCell cPie = new PdfPCell(); cPie.setFixedHeight(height); cPie.setBorder(0);
        cPie.setCellEvent((cell, pos, cvs) -> {
            PdfContentByte cb = cvs[PdfPTable.BACKGROUNDCANVAS];
            cb.setColorFill(BGPAGE); cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight()); cb.fill();
            float cx = pos.getLeft()+pos.getWidth()/2, cy = pos.getBottom()+pos.getHeight()/2;
            float r  = Math.min(pos.getWidth(),pos.getHeight())/2-10;
            drawPastel(cb, cx, cy, r, d, bfB, bfR);
        });
        tp.addCell(cPie);
        PdfPCell cLey = new PdfPCell(); cLey.setBorder(0); cLey.setPaddingLeft(18);
        cLey.setVerticalAlignment(Element.ALIGN_MIDDLE);
        buildLeyenda(cLey, d, bfB, bfR); tp.addCell(cLey);
        return tp;
    }

    // ─── Pastel donut vectorial ───────────────────────────────────────────────
    private void drawPastel(PdfContentByte cb, float cx, float cy, float r, DatosReporte d, BaseFont bfB, BaseFont bfR) {
        Color[] cols  = {C_PENDIENTE, C_APROBADO, C_RECHAZADO};
        int[] vals = {d.kpiEstados.getOrDefault("PENDIENTE",0), d.kpiEstados.getOrDefault("APROBADO",0), d.kpiEstados.getOrDefault("RECHAZADO",0)};
        int total = d.total == 0 ? 1 : d.total;
        float iR = r * 0.42f; int N = 80;
        double startDeg = 90.0;
        for (int i = 0; i < 3; i++) {
            if (vals[i] == 0) continue;
            double sweep = 360.0 * vals[i] / total, endDeg = startDeg - sweep;
            cb.setColorFill(cols[i]); cb.setColorStroke(Color.WHITE); cb.setLineWidth(2f);
            cb.moveTo(cx+(float)(r*Math.cos(Math.toRadians(startDeg))), cy+(float)(r*Math.sin(Math.toRadians(startDeg))));
            for (int s=0;s<=N;s++){double a=Math.toRadians(startDeg-sweep*s/N);cb.lineTo(cx+(float)(r*Math.cos(a)),cy+(float)(r*Math.sin(a)));}
            cb.lineTo(cx+(float)(iR*Math.cos(Math.toRadians(endDeg))),cy+(float)(iR*Math.sin(Math.toRadians(endDeg))));
            for (int s=0;s<=N;s++){double a=Math.toRadians(endDeg+sweep*s/N);cb.lineTo(cx+(float)(iR*Math.cos(a)),cy+(float)(iR*Math.sin(a)));}
            cb.closePath(); cb.fillStroke();
            if (sweep > 14) {
                double midA = Math.toRadians(startDeg - sweep/2);
                float tx=cx+(float)((r*0.68f)*Math.cos(midA)), ty=cy+(float)((r*0.68f)*Math.sin(midA));
                try { cb.beginText(); cb.setFontAndSize(bfB,9f); cb.setColorFill(Color.WHITE);
                    cb.showTextAligned(Element.ALIGN_CENTER, String.format("%.0f%%",100.0*vals[i]/total),tx,ty-3,0); cb.endText(); } catch(Exception ignored){}
            }
            startDeg = endDeg;
        }
        cb.setColorFill(Color.WHITE); cb.setColorStroke(RULE); cb.setLineWidth(0.5f);
        cb.arc(cx-iR,cy-iR,cx+iR,cy+iR,0,360); cb.fillStroke();
        try { cb.beginText(); cb.setFontAndSize(bfB,14f); cb.setColorFill(INK);
            cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(d.total),cx,cy+3,0);
            cb.setFontAndSize(bfR,7f); cb.setColorFill(INK2);
            cb.showTextAligned(Element.ALIGN_CENTER,"total",cx,cy-9,0); cb.endText(); } catch(Exception ignored){}
    }

    private void buildLeyenda(PdfPCell cell, DatosReporte d, BaseFont bfB, BaseFont bfR) {
        int total = d.total==0?1:d.total;
        Color[] cols  = {C_PENDIENTE,C_APROBADO,C_RECHAZADO};
        String[] keys = {"PENDIENTE","APROBADO","RECHAZADO"};
        Font fT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, INK);
        Font fS = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, INK2);
        cell.addElement(new Paragraph("Estados", fT));
        cell.addElement(new Paragraph(" "));
        for (int i=0;i<3;i++){
            int cant=d.kpiEstados.getOrDefault(keys[i],0); double pct=cant*100.0/total;
            PdfPTable row=new PdfPTable(new float[]{5,60,35}); row.setWidthPercentage(100); row.setSpacingBefore(7);
            PdfPCell dot=new PdfPCell(new Phrase(" ")); dot.setBackgroundColor(cols[i]); dot.setBorder(0); dot.setFixedHeight(13); row.addCell(dot);
            PdfPCell lbl=new PdfPCell(); lbl.setBorder(0); lbl.setPaddingLeft(7);
            lbl.addElement(new Paragraph(keys[i],fS)); lbl.addElement(new Paragraph(cant+" registro"+(cant!=1?"s":""),fS)); row.addCell(lbl);
            Font fp=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12f,cols[i]);
            PdfPCell pc=new PdfPCell(new Phrase(String.format("%.1f%%",pct),fp)); pc.setBorder(0); pc.setVerticalAlignment(Element.ALIGN_MIDDLE); row.addCell(pc);
            cell.addElement(row);
        }
    }

    // ─── Barras horizontales a ancho completo con % ──────────────────────────
    private void drawBarsHFullWidth(PdfContentByte cb, Rectangle pos, Map<String,Integer> data,
                                    int maxEntries, int totalGlobal, Color cp, BaseFont bfB, BaseFont bfR) {
        if (data.isEmpty()) return;
        int maxV = data.values().stream().max(Integer::compareTo).orElse(1);
        int total = totalGlobal == 0 ? 1 : totalGlobal;
        float lblW  = 190f;
        float numW  = 38f;
        float pctW  = 46f;
        float barAreaW = pos.getWidth() - lblW - numW - pctW - 16f;
        float x0 = pos.getLeft() + 8;
        float y  = pos.getTop() - 12;
        try {
            int idx = 0;
            for (var e : data.entrySet()) {
                if (idx >= maxEntries) break;
                Color barC = PALETA[idx % PALETA.length];
                int cant  = e.getValue();
                double pct = cant * 100.0 / total;
                float barW = (float) cant / maxV * barAreaW;
                float yBar = y - BAR_H;

                // Fondo alternado
                if (idx % 2 == 0) {
                    cb.setColorFill(new Color(241,245,249));
                    cb.rectangle(pos.getLeft(), yBar - 3, pos.getWidth(), BAR_H + 6); cb.fill();
                }

                // Etiqueta izquierda
                cb.beginText(); cb.setFontAndSize(bfR, 8.5f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_LEFT, truncar(e.getKey(), 26), x0, yBar + BAR_H/3f, 0);
                cb.endText();

                // Barra principal
                if (barW > 0) {
                    cb.setColorFill(new Color(barC.getRed(), barC.getGreen(), barC.getBlue(), 30));
                    cb.rectangle(x0 + lblW + 2, yBar - 2, barW, BAR_H); cb.fill();
                    cb.setColorFill(barC);
                    cb.rectangle(x0 + lblW, yBar, barW, BAR_H); cb.fill();
                    cb.setColorFill(new Color(255,255,255,45));
                    cb.rectangle(x0 + lblW, yBar + BAR_H * 0.6f, barW, BAR_H * 0.35f); cb.fill();
                }

                // Número
                cb.beginText(); cb.setFontAndSize(bfB, 9f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_RIGHT,
                        String.valueOf(cant), x0 + lblW + barAreaW + numW, yBar + BAR_H/3f, 0);
                cb.endText();

                // Porcentaje
                cb.beginText(); cb.setFontAndSize(bfR, 8f); cb.setColorFill(INK2);
                cb.showTextAligned(Element.ALIGN_RIGHT,
                        String.format("%.1f%%", pct), x0 + lblW + barAreaW + numW + pctW, yBar + BAR_H/3f, 0);
                cb.endText();

                y -= (BAR_H + BAR_GAP); idx++;
            }
        } catch (Exception ignored) {}
    }
    // ─── Barras horizontales ──────────────────────────────────────────────────
    private void drawBarsH(PdfContentByte cb, Rectangle pos, Map<String,Integer> data, int maxEntries, BaseFont bfB, BaseFont bfR) {
        if (data.isEmpty()) return;
        int maxV = data.values().stream().max(Integer::compareTo).orElse(1);
        int n = Math.min(data.size(), maxEntries);
        float barH = (pos.getHeight() - 16) / n - 6;
        float lblW = pos.getWidth() * 0.42f;
        float barAreaW = pos.getWidth() - lblW - 36f;
        float x0 = pos.getLeft() + 6, y = pos.getTop() - 8;
        try {
            int idx = 0;
            for (var e : data.entrySet()) {
                if (idx >= maxEntries) break;
                Color barC = PALETA[idx % PALETA.length];
                int cant = e.getValue(); float barW = (float)cant/maxV*barAreaW;
                float yBar = y - barH;
                if (idx%2==0){cb.setColorFill(new Color(241,245,249));cb.rectangle(pos.getLeft(),yBar-2,pos.getWidth(),barH+4);cb.fill();}
                cb.beginText(); cb.setFontAndSize(bfR, 7f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_LEFT, truncar(e.getKey(),22), x0, yBar+barH/2.5f, 0); cb.endText();
                if (barW>0){cb.setColorFill(barC);cb.rectangle(x0+lblW,yBar,barW,barH);cb.fill();}
                cb.beginText(); cb.setFontAndSize(bfB,7.5f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_LEFT,String.valueOf(cant),x0+lblW+barW+4,yBar+barH/2.5f,0); cb.endText();
                y-=(barH+6); idx++;
            }
        } catch (Exception ignored) {}
    }

    // ─── Línea temporal ───────────────────────────────────────────────────────
    private void drawLine(PdfContentByte cb, Rectangle pos, Map<String,Integer> data, Color cp, BaseFont bfB, BaseFont bfR) {
        if (data.isEmpty()) return;
        List<String> meses=new ArrayList<>(data.keySet()); List<Integer> vals=new ArrayList<>(data.values());
        int n=meses.size(), maxV=vals.stream().max(Integer::compareTo).orElse(1);
        float pL=48,pR=20,pT=16,pB=36;
        float cX=pos.getLeft()+pL, cY=pos.getBottom()+pB, cW=pos.getWidth()-pL-pR, cH=pos.getHeight()-pT-pB;
        try {
            cb.setColorFill(Color.WHITE); cb.rectangle(cX,cY,cW,cH); cb.fill();
            for (int gi=0;gi<=4;gi++){
                float gy=cY+cH*gi/4f;
                cb.setColorStroke(RULE); cb.setLineWidth(0.4f); cb.moveTo(cX,gy); cb.lineTo(cX+cW,gy); cb.stroke();
                cb.beginText(); cb.setFontAndSize(bfR,7f); cb.setColorFill(INK2);
                cb.showTextAligned(Element.ALIGN_RIGHT,String.valueOf(maxV*gi/4),cX-6,gy-3,0); cb.endText();
            }
            cb.setColorStroke(RULE); cb.setLineWidth(0.8f); cb.moveTo(cX,cY); cb.lineTo(cX+cW,cY); cb.stroke();
            float[] px=new float[n], py=new float[n];
            for (int i=0;i<n;i++){px[i]=cX+(n==1?cW/2:(float)i/(n-1)*cW); py[i]=cY+(float)vals.get(i)/maxV*cH;}
            if (n>1){
                cb.setColorFill(new Color(cp.getRed(),cp.getGreen(),cp.getBlue(),22));
                cb.moveTo(px[0],cY); for(int i=0;i<n;i++)cb.lineTo(px[i],py[i]); cb.lineTo(px[n-1],cY); cb.closePath(); cb.fill();
            }
            cb.setColorStroke(cp); cb.setLineWidth(2.2f); cb.moveTo(px[0],py[0]);
            for (int i=1;i<n;i++) cb.lineTo(px[i],py[i]); cb.stroke();
            for (int i=0;i<n;i++){
                cb.setColorFill(cp); cb.circle(px[i],py[i],4.5f); cb.fill();
                cb.setColorFill(Color.WHITE); cb.circle(px[i],py[i],2.5f); cb.fill();
                cb.beginText(); cb.setFontAndSize(bfB,8f); cb.setColorFill(INK);
                cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(vals.get(i)),px[i],py[i]+9,0);
                cb.setFontAndSize(bfR,7.5f); cb.setColorFill(INK2);
                String mes=meses.get(i).length()>7?meses.get(i).substring(2):meses.get(i);
                cb.showTextAligned(Element.ALIGN_CENTER,mes,px[i],cY-13,0); cb.endText();
            }
        } catch (Exception ignored) {}
    }

    // ─── Sección ──────────────────────────────────────────────────────────────
    private void sec(Document doc, String titulo, Color cp) throws DocumentException {
        doc.add(new Paragraph("\n"));
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, INK);
        Paragraph p = new Paragraph(titulo, f); p.setSpacingAfter(6); doc.add(p);
        doc.add(new Chunk(new LineSeparator(1.5f, 100, cp, Element.ALIGN_CENTER, 0)));
        doc.add(new Paragraph("\n"));
    }

    // ─── Tabla de detalle ─────────────────────────────────────────────────────
    private void detalle(Document doc, List<Map<String,Object>> rows, Color cp, BaseFont bfB, BaseFont bfR) throws DocumentException {
        String[] hdrs={"ID","Nombres","Apellidos","Cédula","Estado","Fecha","Convocatoria"};
        float[] ws={5,17,17,13,11,12,25};
        PdfPTable t=new PdfPTable(hdrs.length); t.setWidthPercentage(100); t.setWidths(ws); t.setSpacingBefore(4);
        Font fH=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,Color.WHITE);
        for (String h:hdrs){PdfPCell c=new PdfPCell(new Phrase(h,fH));c.setBackgroundColor(cp);c.setPadding(6);c.setBorderColor(cp);t.addCell(c);}
        Font fR=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK);
        Font fRB=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,INK);
        boolean alt=false;
        for (var row:rows){
            Color bg=alt?new Color(248,250,252):Color.WHITE; alt=!alt;
            dc(t,str(row.get("id_prepostulacion")),fRB,bg);
            dc(t,str(row.get("nombres")),fR,bg); dc(t,str(row.get("apellidos")),fR,bg);
            dc(t,str(row.get("identificacion")),fR,bg);
            String est=str(row.get("estadoRevision")).toUpperCase();
            Color bgE=bg; Font fE=fR;
            if("APROBADO".equals(est)){bgE=new Color(220,252,231);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(21,128,61));}
            if("RECHAZADO".equals(est)){bgE=new Color(254,226,226);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(185,28,28));}
            if("PENDIENTE".equals(est)){bgE=new Color(254,249,195);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(133,77,14));}
            dc(t,est,fE,bgE);
            dc(t,formatFecha(row.get("fechaEnvio")),fR,bg);
            dc(t,truncar(str(row.get("tituloConvocatoria")),28),fR,bg);
        }
        doc.add(t);
    }

    private void dc(PdfPTable t, String v, Font f, Color bg){
        PdfPCell c=new PdfPCell(new Phrase(v!=null?v:"—",f));
        c.setPadding(5); c.setBackgroundColor(bg); c.setBorderColor(RULE); c.setBorderWidth(0.5f); t.addCell(c);
    }

    // =========================================================================
    // EXCEL
    // =========================================================================
    private byte[] generarExcel(ReportePrepostulacionConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (XSSFWorkbook wb=new XSSFWorkbook(); ByteArrayOutputStream baos=new ByteArrayOutputStream()) {
            Color cp=parseColor(cfg.getColorPrimario());
            XlsEst ex=new XlsEst(wb,cp);
            xlsResumen(wb.createSheet("Resumen"),cfg,d,ex,wb,cp,usuario);
            if(cfg.isIncluirGraficoEstados()) xlsEstados(wb.createSheet("Por Estado"),d,ex,wb);
            if(cfg.isIncluirGraficoConvocatoria()&&!d.kpiConv.isEmpty()) xlsConv(wb.createSheet("Por Convocatoria"),d,ex,wb);
            if(cfg.isIncluirGraficoTemporal()&&!d.kpiTemp.isEmpty()) xlsTemp(wb.createSheet("Evolucion Temporal"),d,ex,wb);
            if(cfg.isIncluirDetalle()&&!d.rows.isEmpty()) xlsDet(wb.createSheet("Detalle"),d.rows,cfg,ex,wb);
            wb.write(baos); return baos.toByteArray();
        } catch(Exception e){throw new RuntimeException("Excel error",e);}
    }

    private void xlsResumen(XSSFSheet sh, ReportePrepostulacionConfigDTO cfg, DatosReporte d, XlsEst ex, XSSFWorkbook wb, Color cp, Usuario usuario) {
        int f=0;
        Row rT=sh.createRow(f++); rT.setHeightInPoints(36); Cell cT=rT.createCell(0);
        cT.setCellValue(ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Prepostulaciones"); cT.setCellStyle(ex.titulo);
        sh.addMergedRegion(new CellRangeAddress(0,0,0,5));
        if(usuario!=null){Row r=sh.createRow(f++);Cell c=r.createCell(0);c.setCellValue("Generado por: "+usuario.getUsuarioApp()+" <"+usuario.getCorreo()+">");c.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));}
        Row rP=sh.createRow(f++);Cell cP=rP.createCell(0);
        cP.setCellValue("Período: "+(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy")+"   |   "+LocalDateTime.now().format(FMT_TS));
        cP.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));f++;
        String[] kL={"Total","Aprobadas","Rechazadas","Pendientes"};
        int[] kV={d.total,d.aprobadas,d.rechazadas,d.pendientes};
        Color[] kC={cp,C_APROBADO,C_RECHAZADO,C_PENDIENTE};
        Row rH=sh.createRow(f++);for(int i=0;i<4;i++){Cell c=rH.createCell(i);c.setCellValue(kL[i]);c.setCellStyle(xlsCH(wb,kC[i]));}
        Row rV=sh.createRow(f++);rV.setHeightInPoints(30);for(int i=0;i<4;i++){Cell c=rV.createCell(i);c.setCellValue(kV[i]);c.setCellStyle(xlsKN(wb,kC[i]));}
        f+=2;
        // Indicadores secundarios
        Row rHS=sh.createRow(f++);xlsHR(rHS,new String[]{"Indicador","Valor","Descripción"},ex);
        String[][] sec2={
                {"Tasa de aprobación",     String.format("%.1f%%",d.tasaAprobacion),         "% de postulantes aprobados"},
                {"Tiempo prom. revisión",  d.tiempoPromedioRevision<1?"< 1 día":String.format("%.1f días",d.tiempoPromedioRevision), "Días entre envío y resolución"},
                {"Docs. adicionales/post.",String.format("%.1f",d.promedioDocsAdicionales),   "Promedio de archivos extra subidos"},
                {"Completitud documental", String.format("%.0f%% (%d de %d)",d.completitudDocumental,d.postulantesConDocs,d.total), "Postulantes con al menos un doc. adicional"}
        };
        for(String[] row:sec2){Row r=sh.createRow(f++);r.createCell(0).setCellValue(row[0]);r.createCell(1).setCellValue(row[1]);r.createCell(2).setCellValue(row[2]);}
        f+=2;
        // Análisis IA si existe
        if(ok(d.analisisIA)){
            Row rIA=sh.createRow(f++);Cell cIA=rIA.createCell(0);cIA.setCellValue("ANÁLISIS IA (Mistral)");cIA.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));
            Row rIAv=sh.createRow(f++);Cell cIAv=rIAv.createCell(0);cIAv.setCellValue(d.analisisIA);cIAv.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));
        }
        sh.setColumnWidth(0,8000);sh.setColumnWidth(1,5000);sh.setColumnWidth(2,9000);sh.setColumnWidth(3,4000);sh.setColumnWidth(4,4000);sh.setColumnWidth(5,4000);
    }

    private void xlsEstados(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb){
        int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue("Por Estado");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,4));f++;
        Row hdr=sh.createRow(f++);xlsHR(hdr,new String[]{"Estado","Cantidad","Porcentaje","Proporción","Descripción"},ex);
        int tot=d.total==0?1:d.total;Color[]cols={C_PENDIENTE,C_APROBADO,C_RECHAZADO};String[]keys={"PENDIENTE","APROBADO","RECHAZADO"};String[]descs={"En espera","Aprobados","Rechazados"};
        for(int i=0;i<3;i++){int v=d.kpiEstados.getOrDefault(keys[i],0);double pct=v*100.0/tot;Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(keys[i]);r.createCell(1).setCellValue(v);r.createCell(2).setCellValue(String.format("%.2f%%",pct));
            int bars=(int)(pct/5);Cell cB=r.createCell(3);cB.setCellValue("█".repeat(Math.max(bars,0))+"░".repeat(Math.max(20-bars,0)));cB.setCellStyle(xlsCT(wb,cols[i]));r.createCell(4).setCellValue(descs[i]);}
        f++;Row rTot=sh.createRow(f);Cell l=rTot.createCell(0);l.setCellValue("TOTAL");l.setCellStyle(ex.header);Cell v2=rTot.createCell(1);v2.setCellValue(d.total);v2.setCellStyle(ex.header);
        for(int i=0;i<5;i++)sh.autoSizeColumn(i);sh.setColumnWidth(3,9000);
    }
    private void xlsConv(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb){
        int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue("Por Convocatoria");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,4));f++;
        Row hdr=sh.createRow(f++);xlsHR(hdr,new String[]{"Pos.","Convocatoria","Cantidad","% del Total","Visual"},ex);
        int tot=d.total==0?1:d.total,maxV=d.kpiConv.values().stream().max(Integer::compareTo).orElse(1),pos=1;
        for(var e:d.kpiConv.entrySet()){double pct=e.getValue()*100.0/tot;int bars=(int)(e.getValue()*1.0/maxV*20);Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(pos);r.createCell(1).setCellValue(e.getKey());r.createCell(2).setCellValue(e.getValue());r.createCell(3).setCellValue(String.format("%.1f%%",pct));
            Cell cB=r.createCell(4);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(xlsCT(wb,PALETA[(pos-1)%PALETA.length]));pos++;}
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),13000));sh.autoSizeColumn(2);sh.autoSizeColumn(3);sh.setColumnWidth(4,8000);
    }
    private void xlsTemp(XSSFSheet sh, DatosReporte d, XlsEst ex, XSSFWorkbook wb){
        int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue("Evolución Temporal");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,4));f++;
        Row hdr=sh.createRow(f++);xlsHR(hdr,new String[]{"Mes","N°","Tendencia","Δ vs anterior","Acumulado"},ex);
        List<String>ms=new ArrayList<>(d.kpiTemp.keySet());List<Integer>vs=new ArrayList<>(d.kpiTemp.values());
        int maxV=vs.stream().max(Integer::compareTo).orElse(1),acum=0;
        for(int i=0;i<ms.size();i++){int v=vs.get(i);acum+=v;int prev=i>0?vs.get(i-1):0,delta=i>0?v-prev:0;Row r=sh.createRow(f++);
            r.createCell(0).setCellValue(ms.get(i));r.createCell(1).setCellValue(v);
            int bars=(int)(v*1.0/maxV*20);Cell cB=r.createCell(2);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(xlsCT(wb,C_AZUL));
            Cell cD=r.createCell(3);if(i==0){cD.setCellValue("—");}else{cD.setCellValue((delta>=0?"▲ +":"▼ ")+delta);cD.setCellStyle(xlsCT(wb,delta>=0?C_APROBADO:C_RECHAZADO));}
            r.createCell(4).setCellValue(acum);}
        sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(2,9000);sh.autoSizeColumn(3);sh.autoSizeColumn(4);
    }
    private void xlsDet(XSSFSheet sh, List<Map<String,Object>> rows, ReportePrepostulacionConfigDTO cfg, XlsEst ex, XSSFWorkbook wb){
        String[]hs={"ID","Nombres","Apellidos","Identificación","Correo","Estado","Fecha Envío","Convocatoria","Observaciones"};
        Row rH=sh.createRow(0);for(int i=0;i<hs.length;i++){Cell c=rH.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}
        XSSFCellStyle stA=xlsBg(wb,new Color(220,252,231)),stR=xlsBg(wb,new Color(254,226,226)),stP=xlsBg(wb,new Color(254,249,195));
        int f=1;
        for(var row:rows){Row r=sh.createRow(f++);int col=0;
            r.createCell(col++).setCellValue(str(row.get("id_prepostulacion")));r.createCell(col++).setCellValue(str(row.get("nombres")));
            r.createCell(col++).setCellValue(str(row.get("apellidos")));r.createCell(col++).setCellValue(str(row.get("identificacion")));
            r.createCell(col++).setCellValue(str(row.get("correo")));
            Cell cE=r.createCell(col++);String est=str(row.get("estadoRevision")).toUpperCase();cE.setCellValue(est);
            if("APROBADO".equals(est))cE.setCellStyle(stA);else if("RECHAZADO".equals(est))cE.setCellStyle(stR);else if("PENDIENTE".equals(est))cE.setCellStyle(stP);
            r.createCell(col++).setCellValue(formatFecha(row.get("fechaEnvio")));
            r.createCell(col++).setCellValue(str(row.get("tituloConvocatoria")));
            r.createCell(col).setCellValue(str(row.get("observacionesRevision")));}
        for(int i=0;i<hs.length;i++)sh.autoSizeColumn(i);
        sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),7000));sh.setColumnWidth(2,Math.max(sh.getColumnWidth(2),7000));
        sh.setColumnWidth(7,Math.max(sh.getColumnWidth(7),11000));sh.setColumnWidth(8,Math.max(sh.getColumnWidth(8),10000));
        if(cfg.isExcelCongelarEncabezado())sh.createFreezePane(0,1);
        if(cfg.isExcelFiltrosAutomaticos())sh.setAutoFilter(new CellRangeAddress(0,0,0,hs.length-1));
    }

    // ── Excel helpers ─────────────────────────────────────────────────────────
    private void xlsHR(Row r, String[]hs, XlsEst ex){for(int i=0;i<hs.length;i++){Cell c=r.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}}
    private XSSFCellStyle xlsCH(XSSFWorkbook wb,Color color){XSSFCellStyle s=wb.createCellStyle();XSSFFont f=wb.createFont();f.setBold(true);f.setFontHeightInPoints((short)9);f.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));s.setFont(f);s.setFillForegroundColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);s.setAlignment(HorizontalAlignment.CENTER);return s;}
    private XSSFCellStyle xlsKN(XSSFWorkbook wb,Color color){XSSFCellStyle s=wb.createCellStyle();XSSFFont f=wb.createFont();f.setBold(true);f.setFontHeightInPoints((short)16);f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);s.setAlignment(HorizontalAlignment.CENTER);s.setVerticalAlignment(VerticalAlignment.CENTER);return s;}
    private XSSFCellStyle xlsCT(XSSFWorkbook wb,Color color){XSSFCellStyle s=wb.createCellStyle();XSSFFont f=wb.createFont();f.setColor(new XSSFColor(color,new DefaultIndexedColorMap()));s.setFont(f);return s;}
    private XSSFCellStyle xlsBg(XSSFWorkbook wb,Color bg){XSSFCellStyle s=wb.createCellStyle();s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s;}

    private static class XlsEst {
        final XSSFCellStyle titulo,sub,header;
        XlsEst(XSSFWorkbook wb,Color cp){
            XSSFFont fT=wb.createFont();fT.setBold(true);fT.setFontHeightInPoints((short)15);fT.setColor(new XSSFColor(cp,new DefaultIndexedColorMap()));
            XSSFFont fH=wb.createFont();fH.setBold(true);fH.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));
            titulo=mk(wb,fT,null);sub=mk(wb,null,null);
            XSSFCellStyle hs=wb.createCellStyle();hs.setFont(fH);hs.setFillForegroundColor(new XSSFColor(cp,new DefaultIndexedColorMap()));
            hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);hs.setAlignment(HorizontalAlignment.CENTER);header=hs;
        }
        private static XSSFCellStyle mk(XSSFWorkbook wb,XSSFFont font,Color bg){
            XSSFCellStyle s=wb.createCellStyle();if(font!=null)s.setFont(font);
            if(bg!=null){s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}return s;}
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static class DatosReporte {
        Institucion institucion; List<Convocatoria> convocatorias;
        List<Map<String,Object>> rows = new ArrayList<>();
        Map<String,Integer> kpiEstados=new LinkedHashMap<>(), kpiConv=new LinkedHashMap<>(), kpiTemp=new LinkedHashMap<>();
        int total,aprobadas,rechazadas,pendientes,postulantesConDocs;
        double tasaAprobacion,tiempoPromedioRevision,promedioDocsAdicionales,completitudDocumental;
        String analisisIA=null;
    }

    private Date parseDate(String s){if(s==null||s.isBlank())return null;try{return Date.valueOf(LocalDate.parse(s));}catch(Exception e){return null;}}
    private String str(Object o){return o!=null?o.toString():"—";}
    private boolean ok(String s){return s!=null&&!s.isBlank();}
    private String truncar(String s,int max){if(s==null||"—".equals(s))return"—";return s.length()>max?s.substring(0,max)+"…":s;}
    private String formatFecha(Object o){if(o==null)return"—";try{return LocalDate.parse(o.toString().substring(0,10)).format(FMT_FECHA);}catch(Exception e){return o.toString();}}
    private Color parseColor(String hex){if(hex==null||hex.isBlank())return C_APROBADO;try{hex=hex.startsWith("#")?hex.substring(1):hex;return new Color(Integer.parseInt(hex.substring(0,2),16),Integer.parseInt(hex.substring(2,4),16),Integer.parseInt(hex.substring(4,6),16));}catch(Exception e){return C_APROBADO;}}
    private Color darken(Color c,float f){return new Color(Math.max(0,(int)(c.getRed()*(1-f))),Math.max(0,(int)(c.getGreen()*(1-f))),Math.max(0,(int)(c.getBlue()*(1-f))));}
}