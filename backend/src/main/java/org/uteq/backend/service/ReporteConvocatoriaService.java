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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.ReporteConvocatoriaConfigDTO;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.repository.UsuarioRepository;
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
public class ReporteConvocatoriaService {

    private final JdbcTemplate         jdbc;
    private final InstitucionRepository instRepo;
    private final UsuarioRepository     usuarioRepo;

    @Value("${mistral.api.key:}")
    private String mistralApiKey;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Color C_ABIERTA   = new Color( 22, 163,  74);
    private static final Color C_CERRADA   = new Color( 71,  85, 105);
    private static final Color C_CANCELADA = new Color(220,  38,  38);
    private static final Color[] PALETA    = {
            new Color(37,99,235), C_ABIERTA, new Color(234,88,12), new Color(124,58,237),
            new Color(6,148,162), new Color(219,39,119), new Color(5,150,105), new Color(217,119,6)
    };
    private static final Color INK   = new Color( 15,  23,  42);
    private static final Color INK2  = new Color( 71,  85, 105);
    private static final Color RULE  = new Color(226, 232, 240);
    private static final Color BGPAGE= new Color(248, 250, 252);
    private static final float BAR_H  = 26f;
    private static final float BAR_GAP = 8f;

    // =========================================================================
    public byte[] generar(ReporteConvocatoriaConfigDTO cfg) {
        String usuarioApp = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepo.findByUsuarioApp(usuarioApp).orElse(null);
        DatosReporte d = cargarDatos(cfg);
        return "EXCEL".equalsIgnoreCase(cfg.getFormato()) ? generarExcel(cfg, d, usuario) : generarPdf(cfg, d, usuario);
    }

    public String nombreArchivo(ReporteConvocatoriaConfigDTO cfg) {
        return "reporte_convocatorias_" + LocalDateTime.now().format(FMT_NOMBRE)
                + ("EXCEL".equalsIgnoreCase(cfg.getFormato()) ? ".xlsx" : ".pdf");
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================
    private DatosReporte cargarDatos(ReporteConvocatoriaConfigDTO cfg) {
        DatosReporte d = new DatosReporte();
        d.institucion = instRepo.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActivo())).findFirst().orElse(null);
        Date desde = parseDate(cfg.getDesde()), hasta = parseDate(cfg.getHasta());

        StringBuilder sql = new StringBuilder(
                "SELECT cv.id_convocatoria, cv.titulo, cv.descripcion," +
                        " cv.fecha_publicacion AS \"fechaPublicacion\", cv.fecha_inicio AS \"fechaInicio\"," +
                        " cv.fecha_fin AS \"fechaFin\", cv.fecha_limite_documentos AS \"fechaLimiteDocumentos\"," +
                        " cv.estado_convocatoria AS \"estadoConvocatoria\"," +
                        " COUNT(DISTINCT cs.id_solicitud) AS \"totalSolicitudes\"," +
                        " COUNT(DISTINCT ps.id_prepostulacion) AS \"totalPrepostulaciones\"" +
                        " FROM convocatoria cv" +
                        " LEFT JOIN convocatoria_solicitud cs ON cs.id_convocatoria = cv.id_convocatoria" +
                        " LEFT JOIN prepostulacion_solicitud ps ON ps.id_solicitud = cs.id_solicitud" +
                        " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (desde != null) { sql.append(" AND cv.fecha_publicacion >= ?"); params.add(desde); }
        if (hasta != null) { sql.append(" AND cv.fecha_publicacion <= ?"); params.add(hasta); }
        if (ok(cfg.getEstado())) { sql.append(" AND LOWER(cv.estado_convocatoria) = LOWER(?)"); params.add(cfg.getEstado()); }
        sql.append(" GROUP BY cv.id_convocatoria, cv.titulo, cv.descripcion," +
                " cv.fecha_publicacion, cv.fecha_inicio, cv.fecha_fin," +
                " cv.fecha_limite_documentos, cv.estado_convocatoria");
        sql.append(" ORDER BY cv.fecha_publicacion DESC");

        d.rows = jdbc.queryForList(sql.toString(), params.toArray());

        // KPIs
        d.total     = d.rows.size();
        d.abiertas  = (int) d.rows.stream().filter(r -> "abierta".equalsIgnoreCase(str(r.get("estadoConvocatoria")))).count();
        d.cerradas  = (int) d.rows.stream().filter(r -> "cerrada".equalsIgnoreCase(str(r.get("estadoConvocatoria")))).count();
        d.canceladas= (int) d.rows.stream().filter(r -> "cancelada".equalsIgnoreCase(str(r.get("estadoConvocatoria")))).count();

        // Total prepostulaciones y solicitudes acumuladas
        d.totalPrepostulaciones = d.rows.stream()
                .mapToInt(r -> { try { return Integer.parseInt(r.get("totalPrepostulaciones").toString()); } catch (Exception e) { return 0; } }).sum();
        d.totalSolicitudes = d.rows.stream()
                .mapToInt(r -> { try { return Integer.parseInt(r.get("totalSolicitudes").toString()); } catch (Exception e) { return 0; } }).sum();

        // Promedio de prepostulaciones por convocatoria
        d.promedioPrepostulaciones = d.total == 0 ? 0.0 : (double) d.totalPrepostulaciones / d.total;

        // Convocatoria con más prepostulaciones
        d.rows.stream()
                .max(Comparator.comparingInt(r -> { try { return Integer.parseInt(r.get("totalPrepostulaciones").toString()); } catch (Exception e) { return 0; } }))
                .ifPresent(r -> {
                    d.convocatoriaMasActiva = truncar(str(r.get("titulo")), 30);
                    try { d.maxPrepostulaciones = Integer.parseInt(r.get("totalPrepostulaciones").toString()); } catch (Exception e) {}
                });

        // Distribuciones para gráficos
        d.kpiEstados    = calcPorCampo(d.rows, "estadoConvocatoria");
        d.prepostPorConv = d.rows.stream()
                .filter(r -> r.get("totalPrepostulaciones") != null)
                .sorted((a, b) -> {
                    try { return Integer.compare(Integer.parseInt(b.get("totalPrepostulaciones").toString()), Integer.parseInt(a.get("totalPrepostulaciones").toString())); }
                    catch (Exception e) { return 0; }
                })
                .limit(8)
                .collect(Collectors.toMap(
                        r -> truncar(str(r.get("titulo")), 30),
                        r -> { try { return Integer.parseInt(r.get("totalPrepostulaciones").toString()); } catch (Exception e) { return 0; } },
                        (a, b) -> a, LinkedHashMap::new));
        d.kpiTemporal = calcTemporal(d.rows);

        // Análisis IA
        try { d.analisisIA = llamarMistral(d); } catch (Exception ignored) {}
        return d;
    }

    private Map<String,Integer> calcPorCampo(List<Map<String,Object>> rows, String campo) {
        Map<String,Integer> m = new LinkedHashMap<>();
        rows.forEach(r -> { String k = str(r.get(campo)); if (!"—".equals(k)) m.merge(k, 1, Integer::sum); });
        return m;
    }
    private Map<String,Integer> calcTemporal(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        rows.forEach(r -> { Object f = r.get("fechaPublicacion"); if (f != null) m.merge(f.toString().substring(0, 7), 1, Integer::sum); });
        return new TreeMap<>(m);
    }

    // =========================================================================
    // MISTRAL IA
    // =========================================================================
    private String llamarMistral(DatosReporte d) {
        if (!ok(mistralApiKey) || d.total == 0) return null;
        String prompt = String.format(
                "Eres un analista de procesos académicos universitarios. Con los datos de convocatorias docentes, " +
                        "redacta un análisis profesional en español de 3 a 4 oraciones. Sin listas, sin saludos. Solo texto continuo.\\n\\n" +
                        "Datos:\\n" +
                        "- Total convocatorias: %d | Abiertas: %d | Cerradas: %d | Canceladas: %d\\n" +
                        "- Total solicitudes docentes asociadas: %d\\n" +
                        "- Total prepostulaciones recibidas: %d\\n" +
                        "- Promedio de prepostulaciones por convocatoria: %.1f\\n" +
                        "- Convocatoria más activa: %s (%d prepostulaciones)\\n\\n" +
                        "Analiza la demanda docente, el nivel de participación y da una recomendación breve.",
                d.total, d.abiertas, d.cerradas, d.canceladas,
                d.totalSolicitudes, d.totalPrepostulaciones,
                d.promedioPrepostulaciones, d.convocatoriaMasActiva, d.maxPrepostulaciones);
        try {
            String body = "{\"model\":\"mistral-small-latest\"," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" +
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(prompt) + "}]," +
                    "\"max_tokens\":300,\"temperature\":0.35}";
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.mistral.ai/v1/chat/completions"))
                    .header("Content-Type","application/json").header("Authorization","Bearer "+mistralApiKey)
                    .timeout(java.time.Duration.ofSeconds(35))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
            String text = json.path("choices").path(0).path("message").path("content").asText("").trim();
            return text.isBlank() ? null : text;
        } catch (Exception e) { return null; }
    }

    // =========================================================================
    // PDF
    // =========================================================================
    private byte[] generarPdf(ReporteConvocatoriaConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean horiz = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            com.lowagie.text.Rectangle pageSize = horiz ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(pageSize, 44, 44, 52, 46);
            PdfWriter w  = PdfWriter.getInstance(doc, baos);
            Color cp     = parseColor(cfg.getColorPrimario());

            doc.addTitle(ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Convocatorias");
            if (d.institucion != null && d.institucion.getNombre() != null) doc.addCreator(d.institucion.getNombre());
            if (usuario != null) { doc.addAuthor(usuario.getUsuarioApp()); doc.addSubject("Generado por: "+usuario.getUsuarioApp()+" <"+usuario.getCorreo()+">"); }

            w.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter wr, Document dc) {
                    try {
                        PdfContentByte cb = wr.getDirectContent();
                        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                        cb.setColorStroke(RULE); cb.setLineWidth(0.5f);
                        cb.moveTo(dc.left(),dc.bottom()-5); cb.lineTo(dc.right(),dc.bottom()-5); cb.stroke();
                        cb.beginText(); cb.setFontAndSize(bf,7f); cb.setColorFill(INK2);
                        String izq = d.institucion!=null&&d.institucion.getNombre()!=null?d.institucion.getNombre():"";
                        String rev = usuario!=null?usuario.getUsuarioApp()+"  ·  ":"";
                        String der = rev+(cfg.isMostrarFechaGeneracion()?LocalDateTime.now().format(FMT_TS)+"  ·  ":"")+(cfg.isMostrarNumeroPagina()?"Pág. "+wr.getPageNumber():"");
                        cb.showTextAligned(Element.ALIGN_LEFT,izq,dc.left(),dc.bottom()-15,0);
                        cb.showTextAligned(Element.ALIGN_RIGHT,der,dc.right(),dc.bottom()-15,0);
                        cb.endText();
                    } catch (Exception ignored) {}
                }
            });

            doc.open();
            float ph = doc.getPageSize().getHeight(), pw = doc.getPageSize().getWidth();
            float ml = doc.leftMargin(), cw = pw - ml - doc.rightMargin();
            BaseFont bfB = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfR = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

            // ── PORTADA ───────────────────────────────────────────────────────
            if (cfg.isIncluirPortada()) {
                PdfContentByte cv = w.getDirectContent();
                cv.setColorFill(cp); cv.rectangle(0,0,6,ph); cv.fill();
                float sy = ph-80;
                cv.beginText(); cv.setFontAndSize(bfR,8f); cv.setColorFill(INK2);
                cv.showTextAligned(Element.ALIGN_LEFT, d.institucion!=null&&d.institucion.getNombre()!=null?d.institucion.getNombre().toUpperCase():"", ml, sy, 0); cv.endText();
                cv.setColorFill(cp); cv.rectangle(ml,sy-8,cw,2f); cv.fill();
                cv.beginText(); cv.setFontAndSize(bfB,26f); cv.setColorFill(INK);
                cv.showTextAligned(Element.ALIGN_LEFT, ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Convocatorias Docentes", ml, sy-44, 0);
                if (ok(cfg.getSubtitulo())) { cv.setFontAndSize(bfR,11f); cv.setColorFill(INK2); cv.showTextAligned(Element.ALIGN_LEFT,cfg.getSubtitulo(),ml,sy-64,0); }
                cv.endText();

                doc.add(new Paragraph("\n\n\n\n\n\n\n\n\n\n"));

                PdfPTable tmeta = new PdfPTable(3); tmeta.setWidthPercentage(100); tmeta.setSpacingAfter(22);
                Font fML=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK2), fMV=FontFactory.getFont(FontFactory.HELVETICA_BOLD,10f,INK);
                metaCell(tmeta,"PERÍODO",(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy"),fML,fMV,cp);
                metaCell(tmeta,"GENERADO POR",usuario!=null?usuario.getUsuarioApp():"—",fML,fMV,cp);
                metaCell(tmeta,"FECHA",LocalDateTime.now().format(FMT_TS),fML,fMV,cp);
                doc.add(tmeta);

                // KPIs principales
                PdfPTable tk = new PdfPTable(4); tk.setWidthPercentage(100); tk.setSpacingAfter(10);
                kpi(tk,"TOTAL",d.total,cp,new Color(239,246,255));
                kpi(tk,"ABIERTAS",d.abiertas,C_ABIERTA,new Color(240,253,244));
                kpi(tk,"CERRADAS",d.cerradas,C_CERRADA,new Color(248,250,252));
                kpi(tk,"CANCELADAS",d.canceladas,C_CANCELADA,new Color(254,242,242));
                doc.add(tk);

                // Indicadores secundarios
                PdfPTable tsec = kpiSecundarios(d,cp,bfB,bfR,w);
                tsec.setSpacingAfter(20); doc.add(tsec);

                // Pastel estados en portada
                if (d.total > 0 && cfg.isIncluirGraficoEstados()) {
                    doc.add(pastelEstados(d,155,cp,bfB,bfR));
                }
                doc.newPage();
            }

            // ── PÁGINA 2: gráficos ────────────────────────────────────────────
            // Barras: prepostulaciones por convocatoria
            if (cfg.isIncluirGraficoPrepostulaciones() && !d.prepostPorConv.isEmpty()) {
                sec(doc,"Prepostulaciones por convocatoria",cp);
                final int nPC = Math.min(d.prepostPorConv.size(),8);
                float chPC = nPC*(BAR_H+BAR_GAP)+24;
                PdfPTable tb = new PdfPTable(1); tb.setWidthPercentage(100); tb.setSpacingAfter(20);
                PdfPCell cb2 = new PdfPCell(); cb2.setFixedHeight(chPC); cb2.setBorder(0);
                cb2.setCellEvent((cell,pos,cvs) -> {
                    PdfContentByte cb3=cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb3.setColorFill(BGPAGE);cb3.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb3.fill();
                    drawBarsHFW(cb3,pos,d.prepostPorConv,nPC,d.totalPrepostulaciones,cp,bfB,bfR);
                });
                tb.addCell(cb2); doc.add(tb);
            }

            // Evolución temporal (convocatorias publicadas por mes)
            if (cfg.isIncluirGraficoTemporal() && !d.kpiTemporal.isEmpty()) {
                sec(doc,"Convocatorias publicadas por mes",cp);
                PdfPTable tl = new PdfPTable(1); tl.setWidthPercentage(100); tl.setSpacingAfter(20);
                PdfPCell cl = new PdfPCell(); cl.setFixedHeight(185); cl.setBorder(0);
                cl.setCellEvent((cell,pos,cvs) -> {
                    PdfContentByte cb3=cvs[PdfPTable.BACKGROUNDCANVAS];
                    cb3.setColorFill(BGPAGE);cb3.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb3.fill();
                    drawLine(cb3,pos,d.kpiTemporal,cp,bfB,bfR);
                });
                tl.addCell(cl); doc.add(tl);
            }

            // Análisis IA
            if (d.analisisIA != null && !d.analisisIA.isBlank()) {
                sec(doc,"Análisis del período",cp);
                PdfPTable tIA=new PdfPTable(1);tIA.setWidthPercentage(100);tIA.setSpacingAfter(14);
                PdfPCell cIA=new PdfPCell();
                cIA.setBorder(Rectangle.LEFT);cIA.setBorderColorLeft(cp);cIA.setBorderWidthLeft(3f);
                cIA.setBackgroundColor(new Color(249,250,251));cIA.setPadding(16);cIA.setPaddingLeft(18);
                Paragraph pIA=new Paragraph(d.analisisIA.replaceAll("\\r\\n|\\r","\\n").trim(),FontFactory.getFont(FontFactory.HELVETICA,9.5f,INK));
                pIA.setLeading(15f);cIA.addElement(pIA);tIA.addCell(cIA);doc.add(tIA);
                Paragraph pN=new Paragraph("Análisis generado por Mistral AI. Usar como orientación complementaria.",FontFactory.getFont(FontFactory.HELVETICA,7f,INK2));
                pN.setAlignment(Element.ALIGN_RIGHT);pN.setSpacingAfter(16);doc.add(pN);
            }

            // Detalle
            if (cfg.isIncluirDetalle() && !d.rows.isEmpty()) {
                doc.newPage();
                sec(doc,"Detalle de convocatorias  ("+d.total+" registros)",cp);
                tablaDetalle(doc,d.rows,cp,bfB,bfR);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("PDF Convocatorias error: "+e.getMessage(),e); }
    }

    // ─── KPIs secundarios ─────────────────────────────────────────────────────
    private PdfPTable kpiSecundarios(DatosReporte d, Color cp, BaseFont bfB, BaseFont bfR, PdfWriter w) {
        PdfPTable t = new PdfPTable(3); t.setWidthPercentage(100);

        // Total prepostulaciones — gauge
        PdfPCell c1=new PdfPCell();c1.setFixedHeight(82);c1.setBorder(Rectangle.BOX);c1.setBorderColor(RULE);c1.setBorderWidth(0.6f);c1.setBackgroundColor(new Color(239,246,255));
        c1.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];
            drawKpiSec(cb,pos,"PREPOSTULACIONES",String.valueOf(d.totalPrepostulaciones),"total recibidas",cp,bfB,bfR);
            drawGauge(cb,pos,Math.min(d.totalPrepostulaciones/100.0,1.0),cp);});
        t.addCell(c1);

        // Promedio por convocatoria — barras
        PdfPCell c2=new PdfPCell();c2.setFixedHeight(82);c2.setBorder(Rectangle.BOX);c2.setBorderColor(RULE);c2.setBorderWidth(0.6f);c2.setBackgroundColor(new Color(240,253,244));
        c2.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];
            drawKpiSec(cb,pos,"PROMEDIO / CONV.",String.format("%.1f",d.promedioPrepostulaciones),"prepostulaciones por convocatoria",C_ABIERTA,bfB,bfR);
            drawMiniBarras(cb,pos,(int)Math.round(d.promedioPrepostulaciones),20,C_ABIERTA);});
        t.addCell(c2);

        // Convocatoria más activa — anillo
        PdfPCell c3=new PdfPCell();c3.setFixedHeight(82);c3.setBorder(Rectangle.BOX);c3.setBorderColor(RULE);c3.setBorderWidth(0.6f);c3.setBackgroundColor(new Color(254,242,242));
        c3.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];
            double pct=d.totalPrepostulaciones==0?0:(d.maxPrepostulaciones*1.0/d.totalPrepostulaciones);
            drawKpiSec(cb,pos,"MÁS DEMANDADA",truncar(d.convocatoriaMasActiva,16),d.maxPrepostulaciones+" prepostulaciones",C_CANCELADA,bfB,bfR);
            drawAnillo(cb,pos,pct,C_CANCELADA);});
        t.addCell(c3);
        return t;
    }

    // ─── Pastel de estados ────────────────────────────────────────────────────
    private PdfPTable pastelEstados(DatosReporte d, float height, Color cp, BaseFont bfB, BaseFont bfR) {
        Map<String,Color> colorMap=new LinkedHashMap<>();
        colorMap.put("abierta",C_ABIERTA);colorMap.put("cerrada",C_CERRADA);colorMap.put("cancelada",C_CANCELADA);
        PdfPTable tp=new PdfPTable(2);tp.setWidthPercentage(84);tp.setWidths(new float[]{1,1.2f});
        tp.setHorizontalAlignment(Element.ALIGN_CENTER);tp.setSpacingAfter(14);
        PdfPCell cPie=new PdfPCell();cPie.setFixedHeight(height);cPie.setBorder(0);
        cPie.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];
            cb.setColorFill(BGPAGE);cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb.fill();
            float cx=pos.getLeft()+pos.getWidth()/2,cy=pos.getBottom()+pos.getHeight()/2,r=Math.min(pos.getWidth(),pos.getHeight())/2-10;
            drawPastel(cb,cx,cy,r,d.kpiEstados,colorMap,bfB,bfR);});
        tp.addCell(cPie);
        PdfPCell cLey=new PdfPCell();cLey.setBorder(0);cLey.setPaddingLeft(18);cLey.setVerticalAlignment(Element.ALIGN_MIDDLE);
        buildLeyenda(cLey,d.kpiEstados,colorMap,"Estados",bfB,bfR);tp.addCell(cLey);
        return tp;
    }

    // ─── Tabla detalle ────────────────────────────────────────────────────────
    private void tablaDetalle(Document doc, List<Map<String,Object>> rows, Color cp, BaseFont bfB, BaseFont bfR) throws DocumentException {
        String[]hdrs={"Título","Estado","Publicación","Inicio","Fin","Solicitudes","Prepost."};
        float[]ws={30,11,13,13,13,10,10};
        PdfPTable t=new PdfPTable(hdrs.length);t.setWidthPercentage(100);t.setWidths(ws);t.setSpacingBefore(4);
        Font fH=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,Color.WHITE);
        for(String h:hdrs){PdfPCell c=new PdfPCell(new Phrase(h,fH));c.setBackgroundColor(cp);c.setPadding(6);c.setBorderColor(cp);t.addCell(c);}
        Font fR=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK);boolean alt=false;
        for(var row:rows){
            Color bg=alt?new Color(248,250,252):Color.WHITE;alt=!alt;
            dc(t,truncar(str(row.get("titulo")),35),fR,bg);
            String est=str(row.get("estadoConvocatoria")).toLowerCase();Color bgE=bg;Font fE=fR;
            if("abierta".equals(est)){bgE=new Color(220,252,231);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(21,128,61));}
            if("cerrada".equals(est)){bgE=new Color(241,245,249);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,INK2);}
            if("cancelada".equals(est)){bgE=new Color(254,226,226);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(185,28,28));}
            dc(t,est,fE,bgE);
            dc(t,formatFecha(row.get("fechaPublicacion")),fR,bg);
            dc(t,formatFecha(row.get("fechaInicio")),fR,bg);
            dc(t,formatFecha(row.get("fechaFin")),fR,bg);
            dc(t,str(row.get("totalSolicitudes")),fR,bg);
            dc(t,str(row.get("totalPrepostulaciones")),fR,bg);
        }
        doc.add(t);
    }

    // ─── Helpers gráficos ─────────────────────────────────────────────────────
    private void metaCell(PdfPTable t,String lbl,String val,Font fL,Font fV,Color cp){PdfPCell c=new PdfPCell();c.setBorder(Rectangle.BOTTOM);c.setBorderColorBottom(cp);c.setBorderWidthBottom(1.5f);c.setPadding(8);c.setPaddingBottom(10);c.addElement(new Paragraph(lbl,fL));c.addElement(new Paragraph(val,fV));t.addCell(c);}
    private void kpi(PdfPTable t,String label,int valor,Color color,Color bg){Font fL=FontFactory.getFont(FontFactory.HELVETICA,7f,INK2),fV=FontFactory.getFont(FontFactory.HELVETICA_BOLD,20f,color);PdfPTable inner=new PdfPTable(new float[]{4,96});inner.setWidthPercentage(100);PdfPCell bar=new PdfPCell(new Phrase(" "));bar.setBackgroundColor(color);bar.setBorder(0);bar.setPadding(0);inner.addCell(bar);PdfPCell con=new PdfPCell();con.setBorder(0);con.setPadding(10);con.addElement(new Paragraph(label,fL));con.addElement(new Paragraph(String.valueOf(valor),fV));inner.addCell(con);PdfPCell card=new PdfPCell();card.addElement(inner);card.setBackgroundColor(bg);card.setBorderColor(RULE);card.setBorderWidth(0.6f);card.setPadding(0);t.addCell(card);}
    private void sec(Document doc,String titulo,Color cp) throws DocumentException{doc.add(new Paragraph("\n"));Font f=FontFactory.getFont(FontFactory.HELVETICA_BOLD,11f,INK);Paragraph p=new Paragraph(titulo,f);p.setSpacingAfter(6);doc.add(p);doc.add(new Chunk(new LineSeparator(1.5f,100,cp,Element.ALIGN_CENTER,0)));doc.add(new Paragraph("\n"));}
    private void drawKpiSec(PdfContentByte cb,Rectangle pos,String label,String valor,String sub,Color color,BaseFont bfB,BaseFont bfR){try{float x=pos.getLeft()+10,y=pos.getTop()-16;cb.beginText();cb.setFontAndSize(bfR,6.5f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_LEFT,label,x,y,0);cb.setFontAndSize(bfB,11f);cb.setColorFill(color);cb.showTextAligned(Element.ALIGN_LEFT,valor,x,y-16,0);cb.setFontAndSize(bfR,6.5f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_LEFT,sub,x,y-28,0);cb.endText();}catch(Exception ignored){}}
    private void drawGauge(PdfContentByte cb,Rectangle pos,double pct,Color color){float cx=pos.getRight()-28,cy=pos.getBottom()+28,r=20f;cb.setColorStroke(new Color(226,232,240));cb.setLineWidth(4f);cb.setLineCap(1);cb.arc(cx-r,cy-r,cx+r,cy+r,0,180);cb.stroke();if(pct>0){cb.setColorStroke(color);cb.setLineWidth(4f);cb.arc(cx-r,cy-r,cx+r,cy+r,0,(float)(180*pct));cb.stroke();}double ea=Math.PI*pct;float px=cx+(float)(r*Math.cos(ea)),py=cy+(float)(r*Math.sin(ea));cb.setColorFill(color);cb.circle(px,py,3);cb.fill();}
    private void drawMiniBarras(PdfContentByte cb,Rectangle pos,int valor,int maxVal,Color color){int n=5;float bw=8f,gap=4f,totalW=n*bw+(n-1)*gap,sx=pos.getRight()-totalW-10,by=pos.getBottom()+10,maxH=35f;double step=maxVal/(double)n;for(int i=0;i<n;i++){float x=sx+i*(bw+gap);cb.setColorFill(new Color(226,232,240));cb.rectangle(x,by,bw,maxH);cb.fill();float h=(float)(Math.min(valor,step*(i+1))/step*(maxH/n)*n);h=Math.min(h,maxH);h=Math.max(0,h);if(h>0){cb.setColorFill(color);cb.rectangle(x,by,bw,h);cb.fill();}}}
    private void drawAnillo(PdfContentByte cb,Rectangle pos,double pct,Color color){float cx=pos.getRight()-28,cy=pos.getBottom()+28,r=18f,iR=12f;cb.setColorStroke(new Color(226,232,240));cb.setLineWidth(7f);cb.arc(cx-r,cy-r,cx+r,cy+r,0,360);cb.stroke();if(pct>0){cb.setColorStroke(color);cb.setLineWidth(7f);cb.setLineCap(1);cb.arc(cx-r,cy-r,cx+r,cy+r,90,(float)(-360*pct));cb.stroke();}cb.setColorFill(new Color(254,242,242));cb.circle(cx,cy,iR);cb.fill();try{BaseFont bf=BaseFont.createFont(BaseFont.HELVETICA_BOLD,BaseFont.CP1252,false);cb.beginText();cb.setFontAndSize(bf,7f);cb.setColorFill(color);cb.showTextAligned(Element.ALIGN_CENTER,String.format("%.0f%%",pct*100),cx,cy-2,0);cb.endText();}catch(Exception ignored){}}
    private void drawBarsHFW(PdfContentByte cb,Rectangle pos,Map<String,Integer> data,int maxEntries,int totalGlobal,Color cp,BaseFont bfB,BaseFont bfR){if(data.isEmpty())return;int maxV=data.values().stream().max(Integer::compareTo).orElse(1),total=totalGlobal==0?1:totalGlobal;float lblW=175f,numW=38f,pctW=46f,barAreaW=pos.getWidth()-lblW-numW-pctW-16f,x0=pos.getLeft()+8,y=pos.getTop()-12;try{int idx=0;for(var e:data.entrySet()){if(idx>=maxEntries)break;Color barC=PALETA[idx%PALETA.length];int cant=e.getValue();double pct=cant*100.0/total;float barW=(float)cant/maxV*barAreaW,yBar=y-BAR_H;if(idx%2==0){cb.setColorFill(new Color(241,245,249));cb.rectangle(pos.getLeft(),yBar-3,pos.getWidth(),BAR_H+6);cb.fill();}cb.beginText();cb.setFontAndSize(bfR,8.5f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_LEFT,truncar(e.getKey(),24),x0,yBar+BAR_H/3f,0);cb.endText();if(barW>0){cb.setColorFill(new Color(barC.getRed(),barC.getGreen(),barC.getBlue(),30));cb.rectangle(x0+lblW+2,yBar-2,barW,BAR_H);cb.fill();cb.setColorFill(barC);cb.rectangle(x0+lblW,yBar,barW,BAR_H);cb.fill();cb.setColorFill(new Color(255,255,255,45));cb.rectangle(x0+lblW,yBar+BAR_H*0.6f,barW,BAR_H*0.35f);cb.fill();}cb.beginText();cb.setFontAndSize(bfB,9f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_RIGHT,String.valueOf(cant),x0+lblW+barAreaW+numW,yBar+BAR_H/3f,0);cb.endText();cb.beginText();cb.setFontAndSize(bfR,8f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_RIGHT,String.format("%.1f%%",pct),x0+lblW+barAreaW+numW+pctW,yBar+BAR_H/3f,0);cb.endText();y-=(BAR_H+BAR_GAP);idx++;}}catch(Exception ignored){}}
    private void drawLine(PdfContentByte cb,Rectangle pos,Map<String,Integer> data,Color cp,BaseFont bfB,BaseFont bfR){if(data.isEmpty())return;List<String>ks=new ArrayList<>(data.keySet());List<Integer>vs=new ArrayList<>(data.values());int n=ks.size(),maxV=vs.stream().max(Integer::compareTo).orElse(1);float pL=48,pR=20,pT=16,pB=36,cX=pos.getLeft()+pL,cY=pos.getBottom()+pB,cW=pos.getWidth()-pL-pR,cH=pos.getHeight()-pT-pB;try{cb.setColorFill(Color.WHITE);cb.rectangle(cX,cY,cW,cH);cb.fill();for(int gi=0;gi<=4;gi++){float gy=cY+cH*gi/4f;cb.setColorStroke(RULE);cb.setLineWidth(0.4f);cb.moveTo(cX,gy);cb.lineTo(cX+cW,gy);cb.stroke();cb.beginText();cb.setFontAndSize(bfR,7f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_RIGHT,String.valueOf(maxV*gi/4),cX-6,gy-3,0);cb.endText();}cb.setColorStroke(RULE);cb.setLineWidth(0.8f);cb.moveTo(cX,cY);cb.lineTo(cX+cW,cY);cb.stroke();float[]px=new float[n],py=new float[n];for(int i=0;i<n;i++){px[i]=cX+(n==1?cW/2:(float)i/(n-1)*cW);py[i]=cY+(float)vs.get(i)/maxV*cH;}if(n>1){cb.setColorFill(new Color(cp.getRed(),cp.getGreen(),cp.getBlue(),22));cb.moveTo(px[0],cY);for(int i=0;i<n;i++)cb.lineTo(px[i],py[i]);cb.lineTo(px[n-1],cY);cb.closePath();cb.fill();}cb.setColorStroke(cp);cb.setLineWidth(2.2f);cb.moveTo(px[0],py[0]);for(int i=1;i<n;i++)cb.lineTo(px[i],py[i]);cb.stroke();for(int i=0;i<n;i++){cb.setColorFill(cp);cb.circle(px[i],py[i],4.5f);cb.fill();cb.setColorFill(Color.WHITE);cb.circle(px[i],py[i],2.5f);cb.fill();cb.beginText();cb.setFontAndSize(bfB,8f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(vs.get(i)),px[i],py[i]+9,0);cb.setFontAndSize(bfR,7.5f);cb.setColorFill(INK2);String k=ks.get(i);if(k.length()>7)k=k.substring(2);cb.showTextAligned(Element.ALIGN_CENTER,k,px[i],cY-13,0);cb.endText();}}catch(Exception ignored){}}
    private void drawPastel(PdfContentByte cb,float cx,float cy,float r,Map<String,Integer> data,Map<String,Color> colorMap,BaseFont bfB,BaseFont bfR){int total=data.values().stream().mapToInt(Integer::intValue).sum();if(total==0)return;float iR=r*0.42f;int N=80;double startDeg=90.0;int idx=0;for(var e:data.entrySet()){Color col=colorMap.getOrDefault(e.getKey(),PALETA[idx%PALETA.length]);double sweep=360.0*e.getValue()/total,endDeg=startDeg-sweep;cb.setColorFill(col);cb.setColorStroke(Color.WHITE);cb.setLineWidth(2f);cb.moveTo(cx+(float)(r*Math.cos(Math.toRadians(startDeg))),cy+(float)(r*Math.sin(Math.toRadians(startDeg))));for(int s=0;s<=N;s++){double a=Math.toRadians(startDeg-sweep*s/N);cb.lineTo(cx+(float)(r*Math.cos(a)),cy+(float)(r*Math.sin(a)));}cb.lineTo(cx+(float)(iR*Math.cos(Math.toRadians(endDeg))),cy+(float)(iR*Math.sin(Math.toRadians(endDeg))));for(int s=0;s<=N;s++){double a=Math.toRadians(endDeg+sweep*s/N);cb.lineTo(cx+(float)(iR*Math.cos(a)),cy+(float)(iR*Math.sin(a)));}cb.closePath();cb.fillStroke();if(sweep>14){double mA=Math.toRadians(startDeg-sweep/2);float tx=cx+(float)((r*0.68f)*Math.cos(mA)),ty=cy+(float)((r*0.68f)*Math.sin(mA));try{cb.beginText();cb.setFontAndSize(bfB,9f);cb.setColorFill(Color.WHITE);cb.showTextAligned(Element.ALIGN_CENTER,String.format("%.0f%%",100.0*e.getValue()/total),tx,ty-3,0);cb.endText();}catch(Exception ignored){}}startDeg=endDeg;idx++;}cb.setColorFill(Color.WHITE);cb.setColorStroke(RULE);cb.setLineWidth(0.5f);cb.arc(cx-iR,cy-iR,cx+iR,cy+iR,0,360);cb.fillStroke();try{cb.beginText();cb.setFontAndSize(bfB,14f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(total),cx,cy+3,0);cb.setFontAndSize(bfR,7f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_CENTER,"total",cx,cy-9,0);cb.endText();}catch(Exception ignored){}}
    private void buildLeyenda(PdfPCell cell,Map<String,Integer> data,Map<String,Color> colorMap,String titulo,BaseFont bfB,BaseFont bfR){int total=data.values().stream().mapToInt(Integer::intValue).sum();if(total==0)return;Font fT=FontFactory.getFont(FontFactory.HELVETICA_BOLD,9f,INK),fS=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK2);cell.addElement(new Paragraph(titulo,fT));cell.addElement(new Paragraph(" "));int idx=0;for(var e:data.entrySet()){Color col=colorMap.getOrDefault(e.getKey(),PALETA[idx%PALETA.length]);double pct=e.getValue()*100.0/total;PdfPTable row=new PdfPTable(new float[]{5,60,35});row.setWidthPercentage(100);row.setSpacingBefore(7);PdfPCell dot=new PdfPCell(new Phrase(" "));dot.setBackgroundColor(col);dot.setBorder(0);dot.setFixedHeight(13);row.addCell(dot);PdfPCell lbl=new PdfPCell();lbl.setBorder(0);lbl.setPaddingLeft(7);lbl.addElement(new Paragraph(e.getKey(),fS));lbl.addElement(new Paragraph(e.getValue()+" convocatorias",fS));row.addCell(lbl);Font fp=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12f,col);PdfPCell pc=new PdfPCell(new Phrase(String.format("%.1f%%",pct),fp));pc.setBorder(0);pc.setVerticalAlignment(Element.ALIGN_MIDDLE);row.addCell(pc);cell.addElement(row);idx++;}}
    private void dc(PdfPTable t,String v,Font f,Color bg){PdfPCell c=new PdfPCell(new Phrase(v!=null?v:"—",f));c.setPadding(5);c.setBackgroundColor(bg);c.setBorderColor(RULE);c.setBorderWidth(0.5f);t.addCell(c);}

    // =========================================================================
    // EXCEL
    // =========================================================================
    private byte[] generarExcel(ReporteConvocatoriaConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try(XSSFWorkbook wb=new XSSFWorkbook();ByteArrayOutputStream baos=new ByteArrayOutputStream()){
            Color cp=parseColor(cfg.getColorPrimario());XlsEst ex=new XlsEst(wb,cp);
            xlsResumen(wb.createSheet("Resumen"),cfg,d,ex,wb,cp,usuario);
            if(cfg.isIncluirGraficoPrepostulaciones()&&!d.prepostPorConv.isEmpty()) xlsDistribucion(wb.createSheet("Prepost. por Conv."),d.prepostPorConv,"Prepostulaciones por Convocatoria",d.totalPrepostulaciones,ex,wb,cp);
            if(cfg.isIncluirGraficoTemporal()&&!d.kpiTemporal.isEmpty()) xlsTemporal(wb.createSheet("Por Mes"),d,ex,wb,cp);
            if(cfg.isIncluirDetalle()&&!d.rows.isEmpty()) xlsDetalle(wb.createSheet("Detalle"),d.rows,cfg,ex,wb,cp);
            wb.write(baos);return baos.toByteArray();
        }catch(Exception e){throw new RuntimeException("Excel Convocatorias error",e);}
    }

    private void xlsResumen(XSSFSheet sh,ReporteConvocatoriaConfigDTO cfg,DatosReporte d,XlsEst ex,XSSFWorkbook wb,Color cp,Usuario usuario){
        int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(36);Cell cT=rT.createCell(0);cT.setCellValue(ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Convocatorias");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,5));
        if(usuario!=null){Row r=sh.createRow(f++);Cell c=r.createCell(0);c.setCellValue("Generado por: "+usuario.getUsuarioApp()+" <"+usuario.getCorreo()+">");c.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));}
        Row rP=sh.createRow(f++);Cell cP=rP.createCell(0);cP.setCellValue("Período: "+(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy")+"   |   "+LocalDateTime.now().format(FMT_TS));cP.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));f++;
        String[]kL={"Total","Abiertas","Cerradas","Canceladas","Total Prepost.","Promedio/Conv."};
        String[]kV={String.valueOf(d.total),String.valueOf(d.abiertas),String.valueOf(d.cerradas),String.valueOf(d.canceladas),String.valueOf(d.totalPrepostulaciones),String.format("%.1f",d.promedioPrepostulaciones)};
        Color[]kC={cp,C_ABIERTA,C_CERRADA,C_CANCELADA,cp,C_ABIERTA};
        Row rH=sh.createRow(f++);for(int i=0;i<6;i++){Cell c=rH.createCell(i);c.setCellValue(kL[i]);c.setCellStyle(xlsCH(wb,kC[i]));}
        Row rV=sh.createRow(f++);rV.setHeightInPoints(30);for(int i=0;i<6;i++){Cell c=rV.createCell(i);c.setCellValue(kV[i]);c.setCellStyle(ex.sub);}f+=2;
        if(ok(d.analisisIA)){Row rIA=sh.createRow(f++);Cell cIA=rIA.createCell(0);cIA.setCellValue("ANÁLISIS IA (Mistral)");cIA.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));Row rIAv=sh.createRow(f++);Cell cIAv=rIAv.createCell(0);cIAv.setCellValue(d.analisisIA);cIAv.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));}
        for(int i=0;i<6;i++)sh.setColumnWidth(i,6000);
    }
    private void xlsDistribucion(XSSFSheet sh,Map<String,Integer> data,String titulo,int total,XlsEst ex,XSSFWorkbook wb,Color cp){int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue(titulo);cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,3));f++;Row hdr=sh.createRow(f++);String[]hs={"Convocatoria","Prepostulaciones","% del Total","Visual"};for(int i=0;i<4;i++){Cell c=hdr.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}int tot=total==0?1:total,maxV=data.values().stream().max(Integer::compareTo).orElse(1),pos=1;for(var e:data.entrySet()){double pct=e.getValue()*100.0/tot;int bars=(int)(e.getValue()*1.0/maxV*20);Row r=sh.createRow(f++);r.createCell(0).setCellValue(e.getKey());r.createCell(1).setCellValue(e.getValue());r.createCell(2).setCellValue(String.format("%.1f%%",pct));XSSFCellStyle st=xlsBg(wb,PALETA[(pos-1)%PALETA.length]);Cell cB=r.createCell(3);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(st);pos++;}sh.autoSizeColumn(0);sh.setColumnWidth(0,Math.max(sh.getColumnWidth(0),12000));sh.autoSizeColumn(1);sh.autoSizeColumn(2);sh.setColumnWidth(3,8000);}
    private void xlsTemporal(XSSFSheet sh,DatosReporte d,XlsEst ex,XSSFWorkbook wb,Color cp){int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue("Convocatorias por Mes");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,3));f++;Row hdr=sh.createRow(f++);String[]hs={"Mes","Convocatorias","Tendencia","Acumulado"};for(int i=0;i<4;i++){Cell c=hdr.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}List<String>ms=new ArrayList<>(d.kpiTemporal.keySet());List<Integer>vs=new ArrayList<>(d.kpiTemporal.values());int maxV=vs.stream().max(Integer::compareTo).orElse(1),acum=0;for(int i=0;i<ms.size();i++){int v=vs.get(i);acum+=v;Row r=sh.createRow(f++);r.createCell(0).setCellValue(ms.get(i));r.createCell(1).setCellValue(v);int bars=(int)(v*1.0/maxV*20);XSSFCellStyle st=xlsBg(wb,cp);Cell cB=r.createCell(2);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(st);r.createCell(3).setCellValue(acum);}sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(2,9000);sh.autoSizeColumn(3);}
    private void xlsDetalle(XSSFSheet sh,List<Map<String,Object>> rows,ReporteConvocatoriaConfigDTO cfg,XlsEst ex,XSSFWorkbook wb,Color cp){String[]hs={"ID","Título","Estado","Publicación","Inicio","Fin","Lím. Docs","Solicitudes","Prepostulaciones"};Row rH=sh.createRow(0);for(int i=0;i<hs.length;i++){Cell c=rH.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}XSSFCellStyle stA=xlsBg(wb,new Color(220,252,231)),stC=xlsBg(wb,new Color(241,245,249)),stX=xlsBg(wb,new Color(254,226,226));int f=1;for(var row:rows){Row r=sh.createRow(f++);int col=0;r.createCell(col++).setCellValue(str(row.get("id_convocatoria")));r.createCell(col++).setCellValue(str(row.get("titulo")));Cell cE=r.createCell(col++);String est=str(row.get("estadoConvocatoria")).toLowerCase();cE.setCellValue(est);if("abierta".equals(est))cE.setCellStyle(stA);else if("cerrada".equals(est))cE.setCellStyle(stC);else if("cancelada".equals(est))cE.setCellStyle(stX);r.createCell(col++).setCellValue(formatFecha(row.get("fechaPublicacion")));r.createCell(col++).setCellValue(formatFecha(row.get("fechaInicio")));r.createCell(col++).setCellValue(formatFecha(row.get("fechaFin")));r.createCell(col++).setCellValue(formatFecha(row.get("fechaLimiteDocumentos")));r.createCell(col++).setCellValue(str(row.get("totalSolicitudes")));r.createCell(col).setCellValue(str(row.get("totalPrepostulaciones")));}for(int i=0;i<hs.length;i++)sh.autoSizeColumn(i);sh.setColumnWidth(1,Math.max(sh.getColumnWidth(1),14000));if(cfg.isExcelCongelarEncabezado())sh.createFreezePane(0,1);if(cfg.isExcelFiltrosAutomaticos())sh.setAutoFilter(new CellRangeAddress(0,0,0,hs.length-1));}

    private XSSFCellStyle xlsCH(XSSFWorkbook wb,Color c){XSSFCellStyle s=wb.createCellStyle();XSSFFont f=wb.createFont();f.setBold(true);f.setFontHeightInPoints((short)9);f.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));s.setFont(f);s.setFillForegroundColor(new XSSFColor(c,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);s.setAlignment(HorizontalAlignment.CENTER);return s;}
    private XSSFCellStyle xlsBg(XSSFWorkbook wb,Color bg){XSSFCellStyle s=wb.createCellStyle();s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s;}

    private static class XlsEst {
        final XSSFCellStyle titulo,sub,header;
        XlsEst(XSSFWorkbook wb,Color cp){XSSFFont fT=wb.createFont();fT.setBold(true);fT.setFontHeightInPoints((short)15);fT.setColor(new XSSFColor(cp,new DefaultIndexedColorMap()));XSSFFont fH=wb.createFont();fH.setBold(true);fH.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));titulo=mk(wb,fT,null);sub=mk(wb,null,null);XSSFCellStyle hs=wb.createCellStyle();hs.setFont(fH);hs.setFillForegroundColor(new XSSFColor(cp,new DefaultIndexedColorMap()));hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);hs.setAlignment(HorizontalAlignment.CENTER);header=hs;}
        static XSSFCellStyle mk(XSSFWorkbook wb,XSSFFont font,Color bg){XSSFCellStyle s=wb.createCellStyle();if(font!=null)s.setFont(font);if(bg!=null){s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}return s;}
    }

    private static class DatosReporte {
        Institucion institucion; List<Map<String,Object>> rows=new ArrayList<>();
        Map<String,Integer> kpiEstados=new LinkedHashMap<>(),prepostPorConv=new LinkedHashMap<>(),kpiTemporal=new LinkedHashMap<>();
        int total,abiertas,cerradas,canceladas,totalPrepostulaciones,totalSolicitudes,maxPrepostulaciones;
        double promedioPrepostulaciones;
        String convocatoriaMasActiva="—", analisisIA;
    }

    private Date parseDate(String s){if(s==null||s.isBlank())return null;try{return Date.valueOf(LocalDate.parse(s));}catch(Exception e){return null;}}
    private String str(Object o){return o!=null?o.toString():"—";}
    private boolean ok(String s){return s!=null&&!s.isBlank();}
    private String truncar(String s,int max){if(s==null||"—".equals(s))return"—";return s.length()>max?s.substring(0,max)+"…":s;}
    private String formatFecha(Object o){if(o==null)return"—";try{return LocalDate.parse(o.toString().substring(0,10)).format(FMT_FECHA);}catch(Exception e){return o.toString();}}
    private Color parseColor(String hex){if(hex==null||hex.isBlank())return new Color(0,166,62);try{hex=hex.startsWith("#")?hex.substring(1):hex;return new Color(Integer.parseInt(hex.substring(0,2),16),Integer.parseInt(hex.substring(2,4),16),Integer.parseInt(hex.substring(4,6),16));}catch(Exception e){return new Color(0,166,62);}}
}