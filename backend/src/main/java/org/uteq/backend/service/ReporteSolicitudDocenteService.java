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
import org.uteq.backend.dto.ReporteSolicitudDocenteConfigDTO;
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
public class ReporteSolicitudDocenteService {

    private final JdbcTemplate         jdbc;
    private final InstitucionRepository instRepo;
    private final UsuarioRepository     usuarioRepo;

    @Value("${mistral.api.key:}")
    private String mistralApiKey;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Color C_APROBADA  = new Color( 22, 163,  74);
    private static final Color C_RECHAZADA = new Color(220,  38,  38);
    private static final Color C_PENDIENTE = new Color(202, 138,   4);
    private static final Color C_AZUL      = new Color( 37,  99, 235);
    private static final Color C_VIOLETA   = new Color(124,  58, 237);
    private static final Color[] PALETA    = {
            C_AZUL, C_APROBADA, new Color(234,88,12), C_VIOLETA,
            new Color(6,148,162), new Color(219,39,119), new Color(5,150,105), C_RECHAZADA
    };
    private static final Color INK   = new Color( 15,  23,  42);
    private static final Color INK2  = new Color( 71,  85, 105);
    private static final Color RULE  = new Color(226, 232, 240);
    private static final Color BGPAGE= new Color(248, 250, 252);
    private static final float BAR_H  = 26f;
    private static final float BAR_GAP = 8f;

    // =========================================================================
    public byte[] generar(ReporteSolicitudDocenteConfigDTO cfg) {
        String usuarioApp = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepo.findByUsuarioApp(usuarioApp).orElse(null);
        DatosReporte d = cargarDatos(cfg);
        return "EXCEL".equalsIgnoreCase(cfg.getFormato()) ? generarExcel(cfg, d, usuario) : generarPdf(cfg, d, usuario);
    }

    public String nombreArchivo(ReporteSolicitudDocenteConfigDTO cfg) {
        return "reporte_solicitudes_docentes_" + LocalDateTime.now().format(FMT_NOMBRE)
                + ("EXCEL".equalsIgnoreCase(cfg.getFormato()) ? ".xlsx" : ".pdf");
    }

    // =========================================================================
    // CARGA DE DATOS
    // =========================================================================
    private DatosReporte cargarDatos(ReporteSolicitudDocenteConfigDTO cfg) {
        DatosReporte d = new DatosReporte();
        d.institucion = instRepo.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActivo())).findFirst().orElse(null);
        Date desde = parseDate(cfg.getDesde()), hasta = parseDate(cfg.getHasta());

        StringBuilder sql = new StringBuilder(
                "SELECT sd.id_solicitud, sd.fecha_solicitud AS \"fechaSolicitud\"," +
                        " sd.estado_solicitud AS \"estadoSolicitud\"," +
                        " sd.justificacion, sd.cantidad_docentes AS \"cantidadDocentes\"," +
                        " sd.nivel_academico AS \"nivelAcademico\"," +
                        " sd.experiencia_docente_min AS \"expDocente\"," +
                        " sd.experiencia_profesional_min AS \"expProfesional\"," +
                        " sd.observaciones," +
                        " aa.nombres AS \"nombreAutoridad\", aa.apellidos AS \"apellidosAutoridad\"," +
                        " c.nombre_carrera AS \"carrera\"," +
                        " f.nombre_facultad AS \"facultad\"," +
                        " m.nombre_materia AS \"materia\"," +
                        " m.nivel AS \"nivelMateria\"," +
                        " a.nombre_area AS \"area\"" +
                        " FROM solicitud_docente sd" +
                        " JOIN autoridad_academica aa ON aa.id_autoridad = sd.id_autoridad" +
                        " JOIN materia m ON m.id_materia = sd.id_materia" +
                        " JOIN carrera c ON c.id_carrera = m.id_carrera" +
                        " JOIN facultad f ON f.id_facultad = c.id_facultad" +
                        " JOIN area_conocimiento a ON a.id_area = sd.id_area" +
                        " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (desde != null) { sql.append(" AND sd.fecha_solicitud >= ?"); params.add(desde); }
        if (hasta != null) { sql.append(" AND sd.fecha_solicitud <= ?"); params.add(hasta); }
        if (ok(cfg.getEstado()))   { sql.append(" AND LOWER(sd.estado_solicitud) = LOWER(?)"); params.add(cfg.getEstado()); }
        if (ok(cfg.getFacultad())) { sql.append(" AND LOWER(f.nombre_facultad) LIKE LOWER(?)"); params.add("%" + cfg.getFacultad() + "%"); }
        sql.append(" ORDER BY sd.fecha_solicitud DESC");

        d.rows = jdbc.queryForList(sql.toString(), params.toArray());

        // KPIs
        d.total      = d.rows.size();
        d.aprobadas  = (int) d.rows.stream().filter(r -> "aprobada".equalsIgnoreCase(str(r.get("estadoSolicitud")))).count();
        d.rechazadas = (int) d.rows.stream().filter(r -> "rechazada".equalsIgnoreCase(str(r.get("estadoSolicitud")))).count();
        d.pendientes = (int) d.rows.stream().filter(r -> "pendiente".equalsIgnoreCase(str(r.get("estadoSolicitud")))).count();

        // Total docentes solicitados
        d.totalDocentesSolicitados = d.rows.stream()
                .mapToInt(r -> { try { return Integer.parseInt(r.get("cantidadDocentes").toString()); } catch (Exception e) { return 0; } }).sum();

        // Promedio docentes por solicitud
        d.promedioDocentes = d.total == 0 ? 0.0 : (double) d.totalDocentesSolicitados / d.total;

        // Distribuciones
        d.kpiEstados   = calcPorCampo(d.rows, "estadoSolicitud");
        d.kpiCarrera   = calcPorCampoLimitado(d.rows, "carrera",   8);
        d.kpiFacultad  = calcPorCampoLimitado(d.rows, "facultad",  6);
        d.kpiArea      = calcPorCampoLimitado(d.rows, "area",      6);
        d.kpiNivel     = calcPorCampo(d.rows, "nivelAcademico");
        d.kpiTemporal  = calcTemporal(d.rows);

        // Facultad con más solicitudes
        d.kpiFacultad.entrySet().stream().max(Map.Entry.comparingByValue())
                .ifPresent(e -> { d.facultadMasActiva = e.getKey(); d.maxSolicitudesFacultad = e.getValue(); });

        // Análisis IA
        try { d.analisisIA = llamarMistral(d); } catch (Exception ignored) {}
        return d;
    }

    private Map<String,Integer> calcPorCampo(List<Map<String,Object>> rows, String campo) {
        Map<String,Integer> m = new LinkedHashMap<>();
        rows.forEach(r -> { String k = str(r.get(campo)); if (!"—".equals(k)&&!k.isBlank()) m.merge(k, 1, Integer::sum); });
        return m;
    }
    private Map<String,Integer> calcPorCampoLimitado(List<Map<String,Object>> rows, String campo, int max) {
        return calcPorCampo(rows, campo).entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(max)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->a, LinkedHashMap::new));
    }
    private Map<String,Integer> calcTemporal(List<Map<String,Object>> rows) {
        Map<String,Integer> m = new LinkedHashMap<>();
        rows.forEach(r -> { Object f = r.get("fechaSolicitud"); if (f != null) m.merge(f.toString().substring(0, 7), 1, Integer::sum); });
        return new TreeMap<>(m);
    }

    // =========================================================================
    // MISTRAL IA
    // =========================================================================
    private String llamarMistral(DatosReporte d) {
        if (!ok(mistralApiKey) || d.total == 0) return null;
        String topAreas = d.kpiArea.entrySet().stream().limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
        String prompt = String.format(
                "Eres un analista de recursos humanos académicos universitarios. Con los datos de solicitudes docentes, " +
                        "redacta un análisis profesional en español de 3 a 4 oraciones. Sin listas, sin saludos. Solo texto continuo.\\n\\n" +
                        "Datos:\\n" +
                        "- Total solicitudes: %d | Aprobadas: %d | Rechazadas: %d | Pendientes: %d\\n" +
                        "- Total docentes solicitados: %d | Promedio por solicitud: %.1f\\n" +
                        "- Facultad con más solicitudes: %s (%d solicitudes)\\n" +
                        "- Áreas de conocimiento más demandadas: %s\\n\\n" +
                        "Analiza la demanda docente, distribución por facultad y áreas, y da una recomendación breve.",
                d.total, d.aprobadas, d.rechazadas, d.pendientes,
                d.totalDocentesSolicitados, d.promedioDocentes,
                d.facultadMasActiva, d.maxSolicitudesFacultad,
                topAreas.isBlank() ? "N/A" : topAreas);
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
    private byte[] generarPdf(ReporteSolicitudDocenteConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean horiz = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            com.lowagie.text.Rectangle pageSize = horiz ? PageSize.A4.rotate() : PageSize.A4;
            Document doc = new Document(pageSize, 44, 44, 52, 46);
            PdfWriter w  = PdfWriter.getInstance(doc, baos);
            Color cp     = parseColor(cfg.getColorPrimario());

            doc.addTitle(ok(cfg.getTitulo()) ? cfg.getTitulo() : "Reporte de Solicitudes Docentes");
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
            float ph=doc.getPageSize().getHeight(), pw=doc.getPageSize().getWidth();
            float ml=doc.leftMargin(), cw=pw-ml-doc.rightMargin();
            BaseFont bfB=BaseFont.createFont(BaseFont.HELVETICA_BOLD,BaseFont.CP1252,false);
            BaseFont bfR=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,false);

            // ── PORTADA ───────────────────────────────────────────────────────
            if (cfg.isIncluirPortada()) {
                PdfContentByte cv = w.getDirectContent();
                cv.setColorFill(cp); cv.rectangle(0,0,6,ph); cv.fill();
                float sy=ph-80;
                cv.beginText(); cv.setFontAndSize(bfR,8f); cv.setColorFill(INK2);
                cv.showTextAligned(Element.ALIGN_LEFT,d.institucion!=null&&d.institucion.getNombre()!=null?d.institucion.getNombre().toUpperCase():"",ml,sy,0); cv.endText();
                cv.setColorFill(cp); cv.rectangle(ml,sy-8,cw,2f); cv.fill();
                cv.beginText(); cv.setFontAndSize(bfB,26f); cv.setColorFill(INK);
                cv.showTextAligned(Element.ALIGN_LEFT,ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Solicitudes Docentes",ml,sy-44,0);
                if(ok(cfg.getSubtitulo())){cv.setFontAndSize(bfR,11f);cv.setColorFill(INK2);cv.showTextAligned(Element.ALIGN_LEFT,cfg.getSubtitulo(),ml,sy-64,0);}
                cv.endText();

                doc.add(new Paragraph("\n\n\n\n\n\n\n\n\n\n"));

                PdfPTable tmeta=new PdfPTable(3);tmeta.setWidthPercentage(100);tmeta.setSpacingAfter(22);
                Font fML=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK2),fMV=FontFactory.getFont(FontFactory.HELVETICA_BOLD,10f,INK);
                metaCell(tmeta,"PERÍODO",(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy"),fML,fMV,cp);
                metaCell(tmeta,"GENERADO POR",usuario!=null?usuario.getUsuarioApp():"—",fML,fMV,cp);
                metaCell(tmeta,"FECHA",LocalDateTime.now().format(FMT_TS),fML,fMV,cp);
                doc.add(tmeta);

                // KPIs principales
                PdfPTable tk=new PdfPTable(4);tk.setWidthPercentage(100);tk.setSpacingAfter(10);
                kpi(tk,"TOTAL",d.total,cp,new Color(239,246,255));
                kpi(tk,"APROBADAS",d.aprobadas,C_APROBADA,new Color(240,253,244));
                kpi(tk,"RECHAZADAS",d.rechazadas,C_RECHAZADA,new Color(254,242,242));
                kpi(tk,"PENDIENTES",d.pendientes,C_PENDIENTE,new Color(254,252,232));
                doc.add(tk);

                // Indicadores secundarios
                PdfPTable tsec=kpiSecundarios(d,cp,bfB,bfR,w);tsec.setSpacingAfter(20);doc.add(tsec);

                // Pastel estados portada
                if (d.total > 0 && cfg.isIncluirGraficoEstados()) {
                    doc.add(pastelEstados(d,155,cp,bfB,bfR));
                }
                doc.newPage();
            }

            // ── PÁGINA 2: gráficos ────────────────────────────────────────────
            // Barras por carrera
            if (cfg.isIncluirGraficoCarreras() && !d.kpiCarrera.isEmpty()) {
                sec(doc,"Solicitudes por carrera",cp);
                final int nC=Math.min(d.kpiCarrera.size(),8);
                float chC=nC*(BAR_H+BAR_GAP)+24;
                PdfPTable tbC=new PdfPTable(1);tbC.setWidthPercentage(100);tbC.setSpacingAfter(20);
                PdfPCell cC=new PdfPCell();cC.setFixedHeight(chC);cC.setBorder(0);
                cC.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];cb.setColorFill(BGPAGE);cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb.fill();drawBarsHFW(cb,pos,d.kpiCarrera,nC,d.total,cp,bfB,bfR);});
                tbC.addCell(cC);doc.add(tbC);
            }

            // Barras por área de conocimiento
            if (cfg.isIncluirGraficoAreas() && !d.kpiArea.isEmpty()) {
                sec(doc,"Solicitudes por área de conocimiento",cp);
                final int nA=Math.min(d.kpiArea.size(),6);
                float chA=nA*(BAR_H+BAR_GAP)+24;
                PdfPTable tbA=new PdfPTable(1);tbA.setWidthPercentage(100);tbA.setSpacingAfter(20);
                PdfPCell cA=new PdfPCell();cA.setFixedHeight(chA);cA.setBorder(0);
                cA.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];cb.setColorFill(BGPAGE);cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb.fill();drawBarsHFW(cb,pos,d.kpiArea,nA,d.total,C_VIOLETA,bfB,bfR);});
                tbA.addCell(cA);doc.add(tbA);
            }

            // Evolución temporal
            if (cfg.isIncluirGraficoTemporal() && !d.kpiTemporal.isEmpty()) {
                sec(doc,"Evolución temporal de solicitudes",cp);
                PdfPTable tl=new PdfPTable(1);tl.setWidthPercentage(100);tl.setSpacingAfter(20);
                PdfPCell cl=new PdfPCell();cl.setFixedHeight(185);cl.setBorder(0);
                cl.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];cb.setColorFill(BGPAGE);cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb.fill();drawLine(cb,pos,d.kpiTemporal,cp,bfB,bfR);});
                tl.addCell(cl);doc.add(tl);
            }

            // Análisis IA
            if (d.analisisIA != null && !d.analisisIA.isBlank()) {
                sec(doc,"Análisis del período",cp);
                PdfPTable tIA=new PdfPTable(1);tIA.setWidthPercentage(100);tIA.setSpacingAfter(14);
                PdfPCell cIA=new PdfPCell();cIA.setBorder(Rectangle.LEFT);cIA.setBorderColorLeft(cp);cIA.setBorderWidthLeft(3f);cIA.setBackgroundColor(new Color(249,250,251));cIA.setPadding(16);cIA.setPaddingLeft(18);
                Paragraph pIA=new Paragraph(d.analisisIA.replaceAll("\\r\\n|\\r","\\n").trim(),FontFactory.getFont(FontFactory.HELVETICA,9.5f,INK));pIA.setLeading(15f);cIA.addElement(pIA);tIA.addCell(cIA);doc.add(tIA);
                Paragraph pN=new Paragraph("Análisis generado por Mistral AI. Usar como orientación complementaria.",FontFactory.getFont(FontFactory.HELVETICA,7f,INK2));pN.setAlignment(Element.ALIGN_RIGHT);pN.setSpacingAfter(16);doc.add(pN);
            }

            // Detalle
            if (cfg.isIncluirDetalle() && !d.rows.isEmpty()) {
                doc.newPage();
                sec(doc,"Detalle de solicitudes  ("+d.total+" registros)",cp);
                tablaDetalle(doc,d.rows,cp,bfB,bfR);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("PDF Solicitudes error: "+e.getMessage(),e); }
    }

    // ─── KPIs secundarios ─────────────────────────────────────────────────────
    private PdfPTable kpiSecundarios(DatosReporte d, Color cp, BaseFont bfB, BaseFont bfR, PdfWriter w) {
        PdfPTable t = new PdfPTable(3); t.setWidthPercentage(100);

        // Total docentes solicitados — gauge
        PdfPCell c1=new PdfPCell();c1.setFixedHeight(82);c1.setBorder(Rectangle.BOX);c1.setBorderColor(RULE);c1.setBorderWidth(0.6f);c1.setBackgroundColor(new Color(239,246,255));
        c1.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];drawKpiSec(cb,pos,"DOCENTES SOLICITADOS",String.valueOf(d.totalDocentesSolicitados),"plazas totales requeridas",C_AZUL,bfB,bfR);drawGauge(cb,pos,Math.min(d.totalDocentesSolicitados/50.0,1.0),C_AZUL);});
        t.addCell(c1);

        // Promedio docentes por solicitud — barras
        PdfPCell c2=new PdfPCell();c2.setFixedHeight(82);c2.setBorder(Rectangle.BOX);c2.setBorderColor(RULE);c2.setBorderWidth(0.6f);c2.setBackgroundColor(new Color(240,253,244));
        c2.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];drawKpiSec(cb,pos,"PROMEDIO / SOLICITUD",String.format("%.1f docentes",d.promedioDocentes),"plazas promedio por solicitud",C_APROBADA,bfB,bfR);drawMiniBarras(cb,pos,(int)Math.round(d.promedioDocentes),5,C_APROBADA);});
        t.addCell(c2);

        // Facultad más activa — anillo
        PdfPCell c3=new PdfPCell();c3.setFixedHeight(82);c3.setBorder(Rectangle.BOX);c3.setBorderColor(RULE);c3.setBorderWidth(0.6f);c3.setBackgroundColor(new Color(245,243,255));
        c3.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];double pct=d.total==0?0:d.maxSolicitudesFacultad*1.0/d.total;drawKpiSec(cb,pos,"FACULTAD MÁS ACTIVA",truncar(d.facultadMasActiva,16),d.maxSolicitudesFacultad+" solicitudes",C_VIOLETA,bfB,bfR);drawAnillo(cb,pos,pct,C_VIOLETA);});
        t.addCell(c3);
        return t;
    }

    // ─── Pastel de estados ────────────────────────────────────────────────────
    private PdfPTable pastelEstados(DatosReporte d, float height, Color cp, BaseFont bfB, BaseFont bfR) {
        Map<String,Color> colorMap=new LinkedHashMap<>();
        colorMap.put("aprobada",C_APROBADA);colorMap.put("rechazada",C_RECHAZADA);colorMap.put("pendiente",C_PENDIENTE);
        PdfPTable tp=new PdfPTable(2);tp.setWidthPercentage(84);tp.setWidths(new float[]{1,1.2f});tp.setHorizontalAlignment(Element.ALIGN_CENTER);tp.setSpacingAfter(14);
        PdfPCell cPie=new PdfPCell();cPie.setFixedHeight(height);cPie.setBorder(0);
        cPie.setCellEvent((cell,pos,cvs)->{PdfContentByte cb=cvs[PdfPTable.BACKGROUNDCANVAS];cb.setColorFill(BGPAGE);cb.rectangle(pos.getLeft(),pos.getBottom(),pos.getWidth(),pos.getHeight());cb.fill();float cx=pos.getLeft()+pos.getWidth()/2,cy=pos.getBottom()+pos.getHeight()/2,r=Math.min(pos.getWidth(),pos.getHeight())/2-10;drawPastel(cb,cx,cy,r,d.kpiEstados,colorMap,bfB,bfR);});
        tp.addCell(cPie);
        PdfPCell cLey=new PdfPCell();cLey.setBorder(0);cLey.setPaddingLeft(18);cLey.setVerticalAlignment(Element.ALIGN_MIDDLE);buildLeyenda(cLey,d.kpiEstados,colorMap,"Estados",bfB,bfR);tp.addCell(cLey);
        return tp;
    }

    // ─── Tabla detalle ────────────────────────────────────────────────────────
    private void tablaDetalle(Document doc, List<Map<String,Object>> rows, Color cp, BaseFont bfB, BaseFont bfR) throws DocumentException {
        String[]hdrs={"Fecha","Estado","Autoridad","Facultad","Carrera","Materia","Área","Plazas","Nivel"};
        float[]ws={11,10,16,14,14,14,12,7,10};
        PdfPTable t=new PdfPTable(hdrs.length);t.setWidthPercentage(100);t.setWidths(ws);t.setSpacingBefore(4);
        Font fH=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,Color.WHITE);
        for(String h:hdrs){PdfPCell c=new PdfPCell(new Phrase(h,fH));c.setBackgroundColor(cp);c.setPadding(6);c.setBorderColor(cp);t.addCell(c);}
        Font fR=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK);boolean alt=false;
        for(var row:rows){
            Color bg=alt?new Color(248,250,252):Color.WHITE;alt=!alt;
            dc(t,formatFecha(row.get("fechaSolicitud")),fR,bg);
            String est=str(row.get("estadoSolicitud")).toLowerCase();Color bgE=bg;Font fE=fR;
            if("aprobada".equals(est)){bgE=new Color(220,252,231);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(21,128,61));}
            if("rechazada".equals(est)){bgE=new Color(254,226,226);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(185,28,28));}
            if("pendiente".equals(est)){bgE=new Color(254,249,195);fE=FontFactory.getFont(FontFactory.HELVETICA_BOLD,7.5f,new Color(133,77,14));}
            dc(t,est,fE,bgE);
            dc(t,truncar(str(row.get("nombreAutoridad"))+" "+str(row.get("apellidosAutoridad")),20),fR,bg);
            dc(t,truncar(str(row.get("facultad")),16),fR,bg);dc(t,truncar(str(row.get("carrera")),16),fR,bg);
            dc(t,truncar(str(row.get("materia")),16),fR,bg);dc(t,truncar(str(row.get("area")),14),fR,bg);
            dc(t,str(row.get("cantidadDocentes")),fR,bg);dc(t,str(row.get("nivelAcademico")),fR,bg);
        }
        doc.add(t);
    }

    // ─── Helpers gráficos (iguales que en los otros servicios) ─────────────────
    private void metaCell(PdfPTable t,String lbl,String val,Font fL,Font fV,Color cp){PdfPCell c=new PdfPCell();c.setBorder(Rectangle.BOTTOM);c.setBorderColorBottom(cp);c.setBorderWidthBottom(1.5f);c.setPadding(8);c.setPaddingBottom(10);c.addElement(new Paragraph(lbl,fL));c.addElement(new Paragraph(val,fV));t.addCell(c);}
    private void kpi(PdfPTable t,String label,int valor,Color color,Color bg){Font fL=FontFactory.getFont(FontFactory.HELVETICA,7f,INK2),fV=FontFactory.getFont(FontFactory.HELVETICA_BOLD,20f,color);PdfPTable inner=new PdfPTable(new float[]{4,96});inner.setWidthPercentage(100);PdfPCell bar=new PdfPCell(new Phrase(" "));bar.setBackgroundColor(color);bar.setBorder(0);bar.setPadding(0);inner.addCell(bar);PdfPCell con=new PdfPCell();con.setBorder(0);con.setPadding(10);con.addElement(new Paragraph(label,fL));con.addElement(new Paragraph(String.valueOf(valor),fV));inner.addCell(con);PdfPCell card=new PdfPCell();card.addElement(inner);card.setBackgroundColor(bg);card.setBorderColor(RULE);card.setBorderWidth(0.6f);card.setPadding(0);t.addCell(card);}
    private void sec(Document doc,String titulo,Color cp) throws DocumentException{doc.add(new Paragraph("\n"));Font f=FontFactory.getFont(FontFactory.HELVETICA_BOLD,11f,INK);Paragraph p=new Paragraph(titulo,f);p.setSpacingAfter(6);doc.add(p);doc.add(new Chunk(new LineSeparator(1.5f,100,cp,Element.ALIGN_CENTER,0)));doc.add(new Paragraph("\n"));}
    private void drawKpiSec(PdfContentByte cb,Rectangle pos,String label,String valor,String sub,Color color,BaseFont bfB,BaseFont bfR){try{float x=pos.getLeft()+10,y=pos.getTop()-16;cb.beginText();cb.setFontAndSize(bfR,6.5f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_LEFT,label,x,y,0);cb.setFontAndSize(bfB,11f);cb.setColorFill(color);cb.showTextAligned(Element.ALIGN_LEFT,valor,x,y-16,0);cb.setFontAndSize(bfR,6.5f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_LEFT,sub,x,y-28,0);cb.endText();}catch(Exception ignored){}}
    private void drawGauge(PdfContentByte cb,Rectangle pos,double pct,Color color){float cx=pos.getRight()-28,cy=pos.getBottom()+28,r=20f;cb.setColorStroke(new Color(226,232,240));cb.setLineWidth(4f);cb.setLineCap(1);cb.arc(cx-r,cy-r,cx+r,cy+r,0,180);cb.stroke();if(pct>0){cb.setColorStroke(color);cb.setLineWidth(4f);cb.arc(cx-r,cy-r,cx+r,cy+r,0,(float)(180*pct));cb.stroke();}double ea=Math.PI*pct;float px=cx+(float)(r*Math.cos(ea)),py=cy+(float)(r*Math.sin(ea));cb.setColorFill(color);cb.circle(px,py,3);cb.fill();}
    private void drawMiniBarras(PdfContentByte cb,Rectangle pos,int valor,int maxVal,Color color){int n=5;float bw=8f,gap=4f,totalW=n*bw+(n-1)*gap,sx=pos.getRight()-totalW-10,by=pos.getBottom()+10,maxH=35f;double step=maxVal/(double)n;for(int i=0;i<n;i++){float x=sx+i*(bw+gap);cb.setColorFill(new Color(226,232,240));cb.rectangle(x,by,bw,maxH);cb.fill();float h=(float)(Math.min(valor,step*(i+1))/step*(maxH/n)*n);h=Math.min(h,maxH);h=Math.max(0,h);if(h>0){cb.setColorFill(color);cb.rectangle(x,by,bw,h);cb.fill();}}}
    private void drawAnillo(PdfContentByte cb,Rectangle pos,double pct,Color color){float cx=pos.getRight()-28,cy=pos.getBottom()+28,r=18f,iR=12f;cb.setColorStroke(new Color(226,232,240));cb.setLineWidth(7f);cb.arc(cx-r,cy-r,cx+r,cy+r,0,360);cb.stroke();if(pct>0){cb.setColorStroke(color);cb.setLineWidth(7f);cb.setLineCap(1);cb.arc(cx-r,cy-r,cx+r,cy+r,90,(float)(-360*pct));cb.stroke();}cb.setColorFill(new Color(245,243,255));cb.circle(cx,cy,iR);cb.fill();try{BaseFont bf=BaseFont.createFont(BaseFont.HELVETICA_BOLD,BaseFont.CP1252,false);cb.beginText();cb.setFontAndSize(bf,7f);cb.setColorFill(color);cb.showTextAligned(Element.ALIGN_CENTER,String.format("%.0f%%",pct*100),cx,cy-2,0);cb.endText();}catch(Exception ignored){}}
    private void drawBarsHFW(PdfContentByte cb,Rectangle pos,Map<String,Integer> data,int maxEntries,int totalGlobal,Color cp,BaseFont bfB,BaseFont bfR){if(data.isEmpty())return;int maxV=data.values().stream().max(Integer::compareTo).orElse(1),total=totalGlobal==0?1:totalGlobal;float lblW=175f,numW=38f,pctW=46f,barAreaW=pos.getWidth()-lblW-numW-pctW-16f,x0=pos.getLeft()+8,y=pos.getTop()-12;try{int idx=0;for(var e:data.entrySet()){if(idx>=maxEntries)break;Color barC=PALETA[idx%PALETA.length];int cant=e.getValue();double pct=cant*100.0/total;float barW=(float)cant/maxV*barAreaW,yBar=y-BAR_H;if(idx%2==0){cb.setColorFill(new Color(241,245,249));cb.rectangle(pos.getLeft(),yBar-3,pos.getWidth(),BAR_H+6);cb.fill();}cb.beginText();cb.setFontAndSize(bfR,8.5f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_LEFT,truncar(e.getKey(),24),x0,yBar+BAR_H/3f,0);cb.endText();if(barW>0){cb.setColorFill(new Color(barC.getRed(),barC.getGreen(),barC.getBlue(),30));cb.rectangle(x0+lblW+2,yBar-2,barW,BAR_H);cb.fill();cb.setColorFill(barC);cb.rectangle(x0+lblW,yBar,barW,BAR_H);cb.fill();cb.setColorFill(new Color(255,255,255,45));cb.rectangle(x0+lblW,yBar+BAR_H*0.6f,barW,BAR_H*0.35f);cb.fill();}cb.beginText();cb.setFontAndSize(bfB,9f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_RIGHT,String.valueOf(cant),x0+lblW+barAreaW+numW,yBar+BAR_H/3f,0);cb.endText();cb.beginText();cb.setFontAndSize(bfR,8f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_RIGHT,String.format("%.1f%%",pct),x0+lblW+barAreaW+numW+pctW,yBar+BAR_H/3f,0);cb.endText();y-=(BAR_H+BAR_GAP);idx++;}}catch(Exception ignored){}}
    private void drawLine(PdfContentByte cb,Rectangle pos,Map<String,Integer> data,Color cp,BaseFont bfB,BaseFont bfR){if(data.isEmpty())return;List<String>ks=new ArrayList<>(data.keySet());List<Integer>vs=new ArrayList<>(data.values());int n=ks.size(),maxV=vs.stream().max(Integer::compareTo).orElse(1);float pL=48,pR=20,pT=16,pB=36,cX=pos.getLeft()+pL,cY=pos.getBottom()+pB,cW=pos.getWidth()-pL-pR,cH=pos.getHeight()-pT-pB;try{cb.setColorFill(Color.WHITE);cb.rectangle(cX,cY,cW,cH);cb.fill();for(int gi=0;gi<=4;gi++){float gy=cY+cH*gi/4f;cb.setColorStroke(RULE);cb.setLineWidth(0.4f);cb.moveTo(cX,gy);cb.lineTo(cX+cW,gy);cb.stroke();cb.beginText();cb.setFontAndSize(bfR,7f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_RIGHT,String.valueOf(maxV*gi/4),cX-6,gy-3,0);cb.endText();}cb.setColorStroke(RULE);cb.setLineWidth(0.8f);cb.moveTo(cX,cY);cb.lineTo(cX+cW,cY);cb.stroke();float[]px=new float[n],py=new float[n];for(int i=0;i<n;i++){px[i]=cX+(n==1?cW/2:(float)i/(n-1)*cW);py[i]=cY+(float)vs.get(i)/maxV*cH;}if(n>1){cb.setColorFill(new Color(cp.getRed(),cp.getGreen(),cp.getBlue(),22));cb.moveTo(px[0],cY);for(int i=0;i<n;i++)cb.lineTo(px[i],py[i]);cb.lineTo(px[n-1],cY);cb.closePath();cb.fill();}cb.setColorStroke(cp);cb.setLineWidth(2.2f);cb.moveTo(px[0],py[0]);for(int i=1;i<n;i++)cb.lineTo(px[i],py[i]);cb.stroke();for(int i=0;i<n;i++){cb.setColorFill(cp);cb.circle(px[i],py[i],4.5f);cb.fill();cb.setColorFill(Color.WHITE);cb.circle(px[i],py[i],2.5f);cb.fill();cb.beginText();cb.setFontAndSize(bfB,8f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(vs.get(i)),px[i],py[i]+9,0);cb.setFontAndSize(bfR,7.5f);cb.setColorFill(INK2);String k=ks.get(i);if(k.length()>7)k=k.substring(2);cb.showTextAligned(Element.ALIGN_CENTER,k,px[i],cY-13,0);cb.endText();}}catch(Exception ignored){}}
    private void drawPastel(PdfContentByte cb,float cx,float cy,float r,Map<String,Integer> data,Map<String,Color> colorMap,BaseFont bfB,BaseFont bfR){int total=data.values().stream().mapToInt(Integer::intValue).sum();if(total==0)return;float iR=r*0.42f;int N=80;double startDeg=90.0;int idx=0;for(var e:data.entrySet()){Color col=colorMap.getOrDefault(e.getKey(),PALETA[idx%PALETA.length]);double sweep=360.0*e.getValue()/total,endDeg=startDeg-sweep;cb.setColorFill(col);cb.setColorStroke(Color.WHITE);cb.setLineWidth(2f);cb.moveTo(cx+(float)(r*Math.cos(Math.toRadians(startDeg))),cy+(float)(r*Math.sin(Math.toRadians(startDeg))));for(int s=0;s<=N;s++){double a=Math.toRadians(startDeg-sweep*s/N);cb.lineTo(cx+(float)(r*Math.cos(a)),cy+(float)(r*Math.sin(a)));}cb.lineTo(cx+(float)(iR*Math.cos(Math.toRadians(endDeg))),cy+(float)(iR*Math.sin(Math.toRadians(endDeg))));for(int s=0;s<=N;s++){double a=Math.toRadians(endDeg+sweep*s/N);cb.lineTo(cx+(float)(iR*Math.cos(a)),cy+(float)(iR*Math.sin(a)));}cb.closePath();cb.fillStroke();if(sweep>14){double mA=Math.toRadians(startDeg-sweep/2);float tx=cx+(float)((r*0.68f)*Math.cos(mA)),ty=cy+(float)((r*0.68f)*Math.sin(mA));try{cb.beginText();cb.setFontAndSize(bfB,9f);cb.setColorFill(Color.WHITE);cb.showTextAligned(Element.ALIGN_CENTER,String.format("%.0f%%",100.0*e.getValue()/total),tx,ty-3,0);cb.endText();}catch(Exception ignored){}}startDeg=endDeg;idx++;}cb.setColorFill(Color.WHITE);cb.setColorStroke(RULE);cb.setLineWidth(0.5f);cb.arc(cx-iR,cy-iR,cx+iR,cy+iR,0,360);cb.fillStroke();try{cb.beginText();cb.setFontAndSize(bfB,14f);cb.setColorFill(INK);cb.showTextAligned(Element.ALIGN_CENTER,String.valueOf(total),cx,cy+3,0);cb.setFontAndSize(bfR,7f);cb.setColorFill(INK2);cb.showTextAligned(Element.ALIGN_CENTER,"total",cx,cy-9,0);cb.endText();}catch(Exception ignored){}}
    private void buildLeyenda(PdfPCell cell,Map<String,Integer> data,Map<String,Color> colorMap,String titulo,BaseFont bfB,BaseFont bfR){int total=data.values().stream().mapToInt(Integer::intValue).sum();if(total==0)return;Font fT=FontFactory.getFont(FontFactory.HELVETICA_BOLD,9f,INK),fS=FontFactory.getFont(FontFactory.HELVETICA,7.5f,INK2);cell.addElement(new Paragraph(titulo,fT));cell.addElement(new Paragraph(" "));int idx=0;for(var e:data.entrySet()){Color col=colorMap.getOrDefault(e.getKey(),PALETA[idx%PALETA.length]);double pct=e.getValue()*100.0/total;PdfPTable row=new PdfPTable(new float[]{5,60,35});row.setWidthPercentage(100);row.setSpacingBefore(7);PdfPCell dot=new PdfPCell(new Phrase(" "));dot.setBackgroundColor(col);dot.setBorder(0);dot.setFixedHeight(13);row.addCell(dot);PdfPCell lbl=new PdfPCell();lbl.setBorder(0);lbl.setPaddingLeft(7);lbl.addElement(new Paragraph(e.getKey(),fS));lbl.addElement(new Paragraph(e.getValue()+" solicitudes",fS));row.addCell(lbl);Font fp=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12f,col);PdfPCell pc=new PdfPCell(new Phrase(String.format("%.1f%%",pct),fp));pc.setBorder(0);pc.setVerticalAlignment(Element.ALIGN_MIDDLE);row.addCell(pc);cell.addElement(row);idx++;}}
    private void dc(PdfPTable t,String v,Font f,Color bg){PdfPCell c=new PdfPCell(new Phrase(v!=null?v:"—",f));c.setPadding(5);c.setBackgroundColor(bg);c.setBorderColor(RULE);c.setBorderWidth(0.5f);t.addCell(c);}

    // =========================================================================
    // EXCEL
    // =========================================================================
    private byte[] generarExcel(ReporteSolicitudDocenteConfigDTO cfg, DatosReporte d, Usuario usuario) {
        try(XSSFWorkbook wb=new XSSFWorkbook();ByteArrayOutputStream baos=new ByteArrayOutputStream()){
            Color cp=parseColor(cfg.getColorPrimario());XlsEst ex=new XlsEst(wb,cp);
            xlsResumen(wb.createSheet("Resumen"),cfg,d,ex,wb,cp,usuario);
            if(cfg.isIncluirGraficoCarreras()&&!d.kpiCarrera.isEmpty()) xlsDistribucion(wb.createSheet("Por Carrera"),d.kpiCarrera,"Solicitudes por Carrera",d.total,ex,wb);
            if(cfg.isIncluirGraficoAreas()&&!d.kpiArea.isEmpty()) xlsDistribucion(wb.createSheet("Por Área"),d.kpiArea,"Solicitudes por Área",d.total,ex,wb);
            if(cfg.isIncluirGraficoTemporal()&&!d.kpiTemporal.isEmpty()) xlsTemporal(wb.createSheet("Por Mes"),d,ex,wb);
            if(cfg.isIncluirDetalle()&&!d.rows.isEmpty()) xlsDetalle(wb.createSheet("Detalle"),d.rows,cfg,ex,wb);
            wb.write(baos);return baos.toByteArray();
        }catch(Exception e){throw new RuntimeException("Excel Solicitudes error",e);}
    }

    private void xlsResumen(XSSFSheet sh,ReporteSolicitudDocenteConfigDTO cfg,DatosReporte d,XlsEst ex,XSSFWorkbook wb,Color cp,Usuario usuario){
        int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(36);Cell cT=rT.createCell(0);cT.setCellValue(ok(cfg.getTitulo())?cfg.getTitulo():"Reporte de Solicitudes Docentes");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,5));
        if(usuario!=null){Row r=sh.createRow(f++);Cell c=r.createCell(0);c.setCellValue("Generado por: "+usuario.getUsuarioApp()+" <"+usuario.getCorreo()+">");c.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));}
        Row rP=sh.createRow(f++);Cell cP=rP.createCell(0);cP.setCellValue("Período: "+(ok(cfg.getDesde())?cfg.getDesde():"inicio")+" → "+(ok(cfg.getHasta())?cfg.getHasta():"hoy")+"   |   "+LocalDateTime.now().format(FMT_TS));cP.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));f++;
        String[]kL={"Total","Aprobadas","Rechazadas","Pendientes","Docentes Solicitados","Promedio/Solicitud"};
        String[]kV={String.valueOf(d.total),String.valueOf(d.aprobadas),String.valueOf(d.rechazadas),String.valueOf(d.pendientes),String.valueOf(d.totalDocentesSolicitados),String.format("%.1f",d.promedioDocentes)};
        Color[]kC={cp,C_APROBADA,C_RECHAZADA,C_PENDIENTE,C_AZUL,C_VIOLETA};
        Row rH=sh.createRow(f++);for(int i=0;i<6;i++){Cell c=rH.createCell(i);c.setCellValue(kL[i]);c.setCellStyle(xlsCH(wb,kC[i]));}
        Row rV=sh.createRow(f++);rV.setHeightInPoints(30);for(int i=0;i<6;i++){Cell c=rV.createCell(i);c.setCellValue(kV[i]);c.setCellStyle(ex.sub);}f+=2;
        if(ok(d.analisisIA)){Row rIA=sh.createRow(f++);Cell cIA=rIA.createCell(0);cIA.setCellValue("ANÁLISIS IA (Mistral)");cIA.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));Row rIAv=sh.createRow(f++);Cell cIAv=rIAv.createCell(0);cIAv.setCellValue(d.analisisIA);cIAv.setCellStyle(ex.sub);sh.addMergedRegion(new CellRangeAddress(f-1,f-1,0,5));}
        for(int i=0;i<6;i++)sh.setColumnWidth(i,6000);
    }
    private void xlsDistribucion(XSSFSheet sh,Map<String,Integer> data,String titulo,int total,XlsEst ex,XSSFWorkbook wb){int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue(titulo);cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,3));f++;Row hdr=sh.createRow(f++);String[]hs={"Nombre","Solicitudes","% del Total","Visual"};for(int i=0;i<4;i++){Cell c=hdr.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}int tot=total==0?1:total,maxV=data.values().stream().max(Integer::compareTo).orElse(1),pos=1;for(var e:data.entrySet()){double pct=e.getValue()*100.0/tot;int bars=(int)(e.getValue()*1.0/maxV*20);Row r=sh.createRow(f++);r.createCell(0).setCellValue(e.getKey());r.createCell(1).setCellValue(e.getValue());r.createCell(2).setCellValue(String.format("%.1f%%",pct));XSSFCellStyle st=xlsBg(wb,PALETA[(pos-1)%PALETA.length]);Cell cB=r.createCell(3);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(st);pos++;}sh.autoSizeColumn(0);sh.setColumnWidth(0,Math.max(sh.getColumnWidth(0),10000));sh.autoSizeColumn(1);sh.autoSizeColumn(2);sh.setColumnWidth(3,8000);}
    private void xlsTemporal(XSSFSheet sh,DatosReporte d,XlsEst ex,XSSFWorkbook wb){int f=0;Row rT=sh.createRow(f++);rT.setHeightInPoints(26);Cell cT=rT.createCell(0);cT.setCellValue("Solicitudes por Mes");cT.setCellStyle(ex.titulo);sh.addMergedRegion(new CellRangeAddress(0,0,0,3));f++;Row hdr=sh.createRow(f++);String[]hs={"Mes","Solicitudes","Tendencia","Acumulado"};for(int i=0;i<4;i++){Cell c=hdr.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}List<String>ms=new ArrayList<>(d.kpiTemporal.keySet());List<Integer>vs=new ArrayList<>(d.kpiTemporal.values());int maxV=vs.stream().max(Integer::compareTo).orElse(1),acum=0;for(int i=0;i<ms.size();i++){int v=vs.get(i);acum+=v;Row r=sh.createRow(f++);r.createCell(0).setCellValue(ms.get(i));r.createCell(1).setCellValue(v);int bars=(int)(v*1.0/maxV*20);XSSFCellStyle st=xlsBg(wb,C_AZUL);Cell cB=r.createCell(2);cB.setCellValue("█".repeat(Math.max(bars,0)));cB.setCellStyle(st);r.createCell(3).setCellValue(acum);}sh.autoSizeColumn(0);sh.autoSizeColumn(1);sh.setColumnWidth(2,9000);sh.autoSizeColumn(3);}
    private void xlsDetalle(XSSFSheet sh,List<Map<String,Object>> rows,ReporteSolicitudDocenteConfigDTO cfg,XlsEst ex,XSSFWorkbook wb){String[]hs={"ID","Fecha","Estado","Autoridad","Facultad","Carrera","Materia","Área","Plazas","Nivel Acad.","Exp. Docente","Exp. Profesional"};Row rH=sh.createRow(0);for(int i=0;i<hs.length;i++){Cell c=rH.createCell(i);c.setCellValue(hs[i]);c.setCellStyle(ex.header);}XSSFCellStyle stA=xlsBg(wb,new Color(220,252,231)),stR=xlsBg(wb,new Color(254,226,226)),stP=xlsBg(wb,new Color(254,249,195));int f=1;for(var row:rows){Row r=sh.createRow(f++);int col=0;r.createCell(col++).setCellValue(str(row.get("id_solicitud")));r.createCell(col++).setCellValue(formatFecha(row.get("fechaSolicitud")));Cell cE=r.createCell(col++);String est=str(row.get("estadoSolicitud")).toLowerCase();cE.setCellValue(est);if("aprobada".equals(est))cE.setCellStyle(stA);else if("rechazada".equals(est))cE.setCellStyle(stR);else if("pendiente".equals(est))cE.setCellStyle(stP);r.createCell(col++).setCellValue(str(row.get("nombreAutoridad"))+" "+str(row.get("apellidosAutoridad")));r.createCell(col++).setCellValue(str(row.get("facultad")));r.createCell(col++).setCellValue(str(row.get("carrera")));r.createCell(col++).setCellValue(str(row.get("materia")));r.createCell(col++).setCellValue(str(row.get("area")));r.createCell(col++).setCellValue(str(row.get("cantidadDocentes")));r.createCell(col++).setCellValue(str(row.get("nivelAcademico")));r.createCell(col++).setCellValue(str(row.get("expDocente")));r.createCell(col).setCellValue(str(row.get("expProfesional")));}for(int i=0;i<hs.length;i++)sh.autoSizeColumn(i);sh.setColumnWidth(3,Math.max(sh.getColumnWidth(3),9000));sh.setColumnWidth(5,Math.max(sh.getColumnWidth(5),8000));sh.setColumnWidth(6,Math.max(sh.getColumnWidth(6),8000));if(cfg.isExcelCongelarEncabezado())sh.createFreezePane(0,1);if(cfg.isExcelFiltrosAutomaticos())sh.setAutoFilter(new CellRangeAddress(0,0,0,hs.length-1));}

    private XSSFCellStyle xlsCH(XSSFWorkbook wb,Color c){XSSFCellStyle s=wb.createCellStyle();XSSFFont f=wb.createFont();f.setBold(true);f.setFontHeightInPoints((short)9);f.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));s.setFont(f);s.setFillForegroundColor(new XSSFColor(c,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);s.setAlignment(HorizontalAlignment.CENTER);return s;}
    private XSSFCellStyle xlsBg(XSSFWorkbook wb,Color bg){XSSFCellStyle s=wb.createCellStyle();s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);return s;}

    private static class XlsEst {
        final XSSFCellStyle titulo,sub,header;
        XlsEst(XSSFWorkbook wb,Color cp){XSSFFont fT=wb.createFont();fT.setBold(true);fT.setFontHeightInPoints((short)15);fT.setColor(new XSSFColor(cp,new DefaultIndexedColorMap()));XSSFFont fH=wb.createFont();fH.setBold(true);fH.setColor(new XSSFColor(Color.WHITE,new DefaultIndexedColorMap()));titulo=mk(wb,fT,null);sub=mk(wb,null,null);XSSFCellStyle hs=wb.createCellStyle();hs.setFont(fH);hs.setFillForegroundColor(new XSSFColor(cp,new DefaultIndexedColorMap()));hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);hs.setAlignment(HorizontalAlignment.CENTER);header=hs;}
        static XSSFCellStyle mk(XSSFWorkbook wb,XSSFFont font,Color bg){XSSFCellStyle s=wb.createCellStyle();if(font!=null)s.setFont(font);if(bg!=null){s.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));s.setFillPattern(FillPatternType.SOLID_FOREGROUND);}return s;}
    }

    private static class DatosReporte {
        Institucion institucion; List<Map<String,Object>> rows=new ArrayList<>();
        Map<String,Integer> kpiEstados=new LinkedHashMap<>(),kpiCarrera=new LinkedHashMap<>(),kpiFacultad=new LinkedHashMap<>(),kpiArea=new LinkedHashMap<>(),kpiNivel=new LinkedHashMap<>(),kpiTemporal=new LinkedHashMap<>();
        int total,aprobadas,rechazadas,pendientes,totalDocentesSolicitados,maxSolicitudesFacultad;
        double promedioDocentes;
        String facultadMasActiva="—", analisisIA;
    }

    private Date parseDate(String s){if(s==null||s.isBlank())return null;try{return Date.valueOf(LocalDate.parse(s));}catch(Exception e){return null;}}
    private String str(Object o){return o!=null?o.toString():"—";}
    private boolean ok(String s){return s!=null&&!s.isBlank();}
    private String truncar(String s,int max){if(s==null||"—".equals(s))return"—";return s.length()>max?s.substring(0,max)+"…":s;}
    private String formatFecha(Object o){if(o==null)return"—";try{return LocalDate.parse(o.toString().substring(0,10)).format(FMT_FECHA);}catch(Exception e){return o.toString();}}
    private Color parseColor(String hex){if(hex==null||hex.isBlank())return new Color(37,99,235);try{hex=hex.startsWith("#")?hex.substring(1):hex;return new Color(Integer.parseInt(hex.substring(0,2),16),Integer.parseInt(hex.substring(2,4),16),Integer.parseInt(hex.substring(4,6),16));}catch(Exception e){return new Color(37,99,235);}}
}