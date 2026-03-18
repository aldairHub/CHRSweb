package org.uteq.backend.service;

// OpenPDF
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
// Lombok
import lombok.RequiredArgsConstructor;
// Apache POI
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// Spring
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
// Proyecto
import org.uteq.backend.dto.ReporteAuditoriaConfigDTO;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.InstitucionRepository;
// Java
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteAuditoriaService {

    private final JdbcTemplate         jdbc;
    private final InstitucionRepository instRepo;

    private static final DateTimeFormatter FMT_FECHA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_TS     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // =========================================================================
    // ENTRADA PRINCIPAL
    // =========================================================================

    public byte[] generar(ReporteAuditoriaConfigDTO cfg) {
        // Cargar datos via SPs — nunca SELECTs directos
        DatosReporte datos = cargarDatos(cfg);

        return "EXCEL".equalsIgnoreCase(cfg.getFormato())
                ? generarExcel(cfg, datos)
                : generarPdf(cfg, datos);
    }

    public String nombreArchivo(ReporteAuditoriaConfigDTO cfg) {
        String tipo   = cfg.getTipoReporte() != null ? cfg.getTipoReporte().toLowerCase() : "auditoria";
        String ts     = LocalDateTime.now().format(FMT_NOMBRE);
        String ext    = "EXCEL".equalsIgnoreCase(cfg.getFormato()) ? "xlsx" : "pdf";
        return "reporte_auditoria_" + tipo + "_" + ts + "." + ext;
    }

    // =========================================================================
    // CARGA DE DATOS VÍA SPs
    // =========================================================================

    private DatosReporte cargarDatos(ReporteAuditoriaConfigDTO cfg) {
        DatosReporte d = new DatosReporte();

        Date desde = cfg.getDesde() != null && !cfg.getDesde().isBlank()
                ? Date.valueOf(LocalDate.parse(cfg.getDesde())) : null;
        Date hasta = cfg.getHasta() != null && !cfg.getHasta().isBlank()
                ? Date.valueOf(LocalDate.parse(cfg.getHasta())) : null;
        int  limite = cfg.getLimite() != null ? cfg.getLimite() : 500;

        // Institución (para encabezado)
        d.institucion = instRepo.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getActivo())).findFirst().orElse(null);

        // Resumen ejecutivo — siempre se carga para la portada
        if (cfg.isIncluirPortada() || cfg.isIncluirKpis()) {
            d.resumen = jdbc.queryForMap(
                    "SELECT * FROM public.sp_reporte_resumen(?, ?)", desde, hasta);
        }

        // Datos de login
        boolean necesitaLogin = "LOGIN".equals(cfg.getTipoReporte())
                || "COMPLETO".equals(cfg.getTipoReporte());
        if (necesitaLogin && cfg.isIncluirDetalle()) {
            d.login = jdbc.queryForList(
                    "SELECT * FROM public.sp_reporte_login(?, ?, ?, ?, ?)",
                    desde, hasta,
                    blank(cfg.getUsuarioApp()),
                    blank(cfg.getResultado()),
                    limite
            );
        }

        // Datos de cambios
        boolean necesitaCambios = "CAMBIOS".equals(cfg.getTipoReporte())
                || "COMPLETO".equals(cfg.getTipoReporte());
        if (necesitaCambios && cfg.isIncluirDetalle()) {
            d.cambios = jdbc.queryForList(
                    "SELECT * FROM public.sp_reporte_cambios(?, ?, ?, ?, ?, ?)",
                    desde, hasta,
                    blank(cfg.getTabla()),
                    blank(cfg.getOperacion()),
                    blank(cfg.getUsuarioApp()),
                    limite
            );
        }

        return d;
    }

    // =========================================================================
    // PDF — OpenPDF
    // =========================================================================

    private byte[] generarPdf(ReporteAuditoriaConfigDTO cfg, DatosReporte datos) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Orientación
            boolean horizontal = "HORIZONTAL".equalsIgnoreCase(cfg.getOrientacion());
            Rectangle pageSize = horizontal ? PageSize.A4.rotate() : PageSize.A4;

            Document doc = new Document(pageSize, 40, 40, 60, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            // Pie de página
            if (cfg.isMostrarNumeroPagina() || cfg.isMostrarFechaGeneracion()) {
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter w, Document d) {
                        PdfContentByte cb = w.getDirectContent();
                        BaseFont bf;
                        try { bf = BaseFont.createFont(); } catch (Exception e) { return; }
                        cb.beginText();
                        cb.setFontAndSize(bf, 8);
                        cb.setColorFill(new Color(150, 150, 150));
                        String pie = "";
                        if (cfg.isMostrarFechaGeneracion())
                            pie += "Generado: " + LocalDateTime.now().format(FMT_TS) + "  ";
                        if (cfg.isMostrarNumeroPagina())
                            pie += "Página " + w.getPageNumber();
                        cb.showTextAligned(Element.ALIGN_CENTER, pie,
                                (d.left() + d.right()) / 2, d.bottom() - 15, 0);
                        cb.endText();
                    }
                });
            }

            doc.open();

            Color colorPrimario = parseColor(cfg.getColorPrimario());

            // ── Portada ──────────────────────────────────────────────────
            if (cfg.isIncluirPortada()) {
                agregarPortada(doc, cfg, datos, colorPrimario);
                doc.newPage();
            }

            // ── Sección login ────────────────────────────────────────────
            boolean conLogin = "LOGIN".equals(cfg.getTipoReporte()) || "COMPLETO".equals(cfg.getTipoReporte());
            if (conLogin) {
                if (cfg.isIncluirKpis() && datos.resumen != null) {
                    agregarSeccionPdf(doc, "Resumen de Accesos", colorPrimario);
                    agregarKpisLoginPdf(doc, datos.resumen, colorPrimario);
                }
                if (cfg.isIncluirDetalle() && datos.login != null && !datos.login.isEmpty()) {
                    agregarSeccionPdf(doc, "Detalle de Accesos", colorPrimario);
                    agregarTablaLoginPdf(doc, datos.login, colorPrimario);
                }
            }

            // ── Sección cambios ──────────────────────────────────────────
            boolean conCambios = "CAMBIOS".equals(cfg.getTipoReporte()) || "COMPLETO".equals(cfg.getTipoReporte());
            if (conCambios) {
                if (cfg.isIncluirKpis() && datos.resumen != null) {
                    agregarSeccionPdf(doc, "Resumen de Cambios en Datos", colorPrimario);
                    agregarKpisCambiosPdf(doc, datos.resumen, colorPrimario);
                }
                if (cfg.isIncluirDetalle() && datos.cambios != null && !datos.cambios.isEmpty()) {
                    agregarSeccionPdf(doc, "Detalle de Cambios en Datos", colorPrimario);
                    agregarTablaCambiosPdf(doc, datos.cambios, colorPrimario);
                }
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de auditoría", e);
        }
    }

    private void agregarPortada(Document doc, ReporteAuditoriaConfigDTO cfg,
                                DatosReporte datos, Color colorPrimario) throws DocumentException {

        // Encabezado institución
        if (cfg.isIncluirInstitucion() && datos.institucion != null) {
            Institucion inst = datos.institucion;
            Font fNombre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, colorPrimario);
            Font fSub    = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

            // Nombre del sistema (appName) — si no existe, se usa el nombre de la institución
            String nombreSistema = (inst.getAppName() != null && !inst.getAppName().isBlank())
                    ? inst.getAppName()
                    : (inst.getNombre() != null ? inst.getNombre() : "Sistema");
            Paragraph pSistema = new Paragraph(nombreSistema, fNombre);
            pSistema.setAlignment(Element.ALIGN_CENTER);
            doc.add(pSistema);

            // Nombre de la institución (si es distinto del sistema)
            if (inst.getAppName() != null && !inst.getAppName().isBlank()
                    && inst.getNombre() != null && !inst.getNombre().isBlank()) {
                Paragraph pInst = new Paragraph(inst.getNombre(), fSub);
                pInst.setAlignment(Element.ALIGN_CENTER);
                doc.add(pInst);
            }
            doc.add(new Paragraph("\n"));
        }

        // Línea separadora
        LineSeparator sep = new LineSeparator(2, 100, colorPrimario, Element.ALIGN_CENTER, 0);
        doc.add(new Chunk(sep));
        doc.add(new Paragraph("\n\n"));

        // Título principal
        String titulo = cfg.getTitulo() != null && !cfg.getTitulo().isBlank()
                ? cfg.getTitulo() : "Reporte de Auditoría del Sistema";
        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, colorPrimario);
        Paragraph pTitulo = new Paragraph(titulo, fTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitulo);

        // Subtítulo
        if (cfg.getSubtitulo() != null && !cfg.getSubtitulo().isBlank()) {
            Font fSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
            Paragraph pSub = new Paragraph(cfg.getSubtitulo(), fSubtitulo);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingBefore(8);
            doc.add(pSub);
        }

        // Período
        Font fInfo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        String periodo = "Período: "
                + (cfg.getDesde() != null ? cfg.getDesde() : "inicio")
                + " — "
                + (cfg.getHasta() != null ? cfg.getHasta() : "hoy");
        Paragraph pPeriodo = new Paragraph(periodo, fInfo);
        pPeriodo.setAlignment(Element.ALIGN_CENTER);
        pPeriodo.setSpacingBefore(12);
        doc.add(pPeriodo);

        doc.add(new Paragraph("\n\n"));

        // Resumen ejecutivo en portada
        if (cfg.isIncluirKpis() && datos.resumen != null) {
            Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, colorPrimario);
            doc.add(new Paragraph("Resumen ejecutivo", fSeccion));
            doc.add(new Paragraph("\n"));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(8);

            agregarCeldaKpi(tabla, "Accesos totales",
                    str(datos.resumen.get("login_total")), colorPrimario);
            agregarCeldaKpi(tabla, "Accesos exitosos",
                    str(datos.resumen.get("login_exitosos")), new Color(22, 163, 74));
            agregarCeldaKpi(tabla, "Accesos fallidos",
                    str(datos.resumen.get("login_fallidos")), new Color(220, 38, 38));
            agregarCeldaKpi(tabla, "Tasa de éxito",
                    str(datos.resumen.get("login_tasa")) + "%", colorPrimario);

            doc.add(tabla);
            doc.add(new Paragraph("\n"));

            PdfPTable tabla2 = new PdfPTable(4);
            tabla2.setWidthPercentage(100);

            agregarCeldaKpi(tabla2, "Total cambios",
                    str(datos.resumen.get("cambios_total")), colorPrimario);
            agregarCeldaKpi(tabla2, "INSERT",
                    str(datos.resumen.get("cambios_insert")), new Color(22, 163, 74));
            agregarCeldaKpi(tabla2, "UPDATE",
                    str(datos.resumen.get("cambios_update")), new Color(37, 99, 235));
            agregarCeldaKpi(tabla2, "DELETE",
                    str(datos.resumen.get("cambios_delete")), new Color(220, 38, 38));

            doc.add(tabla2);
        }

        // Generado por + fecha
        Font fGen = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        String textoGen = "Generado el " + LocalDateTime.now().format(FMT_TS);
        if (cfg.getGeneradoPor() != null && !cfg.getGeneradoPor().isBlank()) {
            textoGen += "  |  Generado por: " + cfg.getGeneradoPor();
        }
        Paragraph pGen = new Paragraph(textoGen, fGen);
        pGen.setAlignment(Element.ALIGN_RIGHT);
        pGen.setSpacingBefore(20);
        doc.add(pGen);
    }

    private void agregarSeccionPdf(Document doc, String titulo, Color color)
            throws DocumentException {
        doc.add(new Paragraph("\n"));
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, color);
        Paragraph p = new Paragraph(titulo, f);
        p.setSpacingAfter(8);
        doc.add(p);
        LineSeparator sep = new LineSeparator(1, 100, color, Element.ALIGN_CENTER, 0);
        doc.add(new Chunk(sep));
        doc.add(new Paragraph("\n"));
    }

    private void agregarKpisLoginPdf(Document doc, Map<String, Object> resumen, Color color)
            throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        agregarCeldaKpi(t, "Total",     str(resumen.get("login_total")),    color);
        agregarCeldaKpi(t, "Exitosos",  str(resumen.get("login_exitosos")), new Color(22,163,74));
        agregarCeldaKpi(t, "Fallidos",  str(resumen.get("login_fallidos")), new Color(220,38,38));
        agregarCeldaKpi(t, "Tasa éxito",str(resumen.get("login_tasa"))+"%", color);
        doc.add(t);
        doc.add(new Paragraph("\n"));
    }

    private void agregarKpisCambiosPdf(Document doc, Map<String, Object> resumen, Color color)
            throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        agregarCeldaKpi(t, "Total",   str(resumen.get("cambios_total")),  color);
        agregarCeldaKpi(t, "INSERT",  str(resumen.get("cambios_insert")), new Color(22,163,74));
        agregarCeldaKpi(t, "UPDATE",  str(resumen.get("cambios_update")), new Color(37,99,235));
        agregarCeldaKpi(t, "DELETE",  str(resumen.get("cambios_delete")), new Color(220,38,38));
        doc.add(t);
        doc.add(new Paragraph("\n"));
    }

    private void agregarTablaLoginPdf(Document doc, List<Map<String, Object>> rows, Color color)
            throws DocumentException {
        String[] headers = {"Fecha", "Usuario", "Resultado", "Motivo", "IP"};
        float[]  widths  = {22, 28, 14, 22, 14};
        PdfPTable t = crearTablaPdf(headers, widths, color);
        Font fCell = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

        for (Map<String, Object> row : rows) {
            t.addCell(celdaDato(str(row.get("fecha")), fCell));
            t.addCell(celdaDato(str(row.get("usuario_app")), fCell));
            PdfPCell cRes = celdaDato(str(row.get("resultado")), fCell);
            String res = str(row.get("resultado"));
            if ("SUCCESS".equals(res)) cRes.setBackgroundColor(new Color(220, 252, 231));
            if ("FAIL".equals(res))    cRes.setBackgroundColor(new Color(254, 226, 226));
            t.addCell(cRes);
            t.addCell(celdaDato(str(row.get("motivo")), fCell));
            t.addCell(celdaDato(str(row.get("ip_cliente")), fCell));
        }
        doc.add(t);
    }

    private void agregarTablaCambiosPdf(Document doc, List<Map<String, Object>> rows, Color color)
            throws DocumentException {
        String[] headers = {"Fecha", "Tabla", "ID", "Op.", "Campo", "Antes", "Después", "Usuario app", "Usuario BD"};
        float[]  widths  = {14, 10, 5, 6, 10, 13, 13, 15, 14};
        PdfPTable t = crearTablaPdf(headers, widths, color);
        Font fCell = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);

        for (Map<String, Object> row : rows) {
            t.addCell(celdaDato(str(row.get("fecha")), fCell));
            t.addCell(celdaDato(str(row.get("tabla")), fCell));
            t.addCell(celdaDato(str(row.get("id_registro")), fCell));

            PdfPCell cOp = celdaDato(str(row.get("operacion")), fCell);
            String op = str(row.get("operacion"));
            if ("INSERT".equals(op)) cOp.setBackgroundColor(new Color(220,252,231));
            if ("UPDATE".equals(op)) cOp.setBackgroundColor(new Color(219,234,254));
            if ("DELETE".equals(op)) cOp.setBackgroundColor(new Color(254,226,226));
            t.addCell(cOp);

            t.addCell(celdaDato(str(row.get("campo")), fCell));
            t.addCell(celdaDato(truncar(str(row.get("valor_antes")), 30), fCell));
            t.addCell(celdaDato(truncar(str(row.get("valor_despues")), 30), fCell));
            t.addCell(celdaDato(str(row.get("usuario_app")), fCell));
            String usuBd = str(row.get("usuario_bd"));
            t.addCell(celdaDato("-".equals(usuBd) ? "—" : usuBd, fCell));
        }
        doc.add(t);
    }

    // =========================================================================
    // EXCEL — Apache POI
    // =========================================================================

    private byte[] generarExcel(ReporteAuditoriaConfigDTO cfg, DatosReporte datos) {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Color colorPrimario = parseColor(cfg.getColorPrimario());

            // Estilos base
            ExcelEstilos estilos = new ExcelEstilos(wb, colorPrimario);

            boolean conLogin   = "LOGIN".equals(cfg.getTipoReporte())   || "COMPLETO".equals(cfg.getTipoReporte());
            boolean conCambios = "CAMBIOS".equals(cfg.getTipoReporte()) || "COMPLETO".equals(cfg.getTipoReporte());

            // ── Hoja resumen ─────────────────────────────────────────────
            if (cfg.isIncluirPortada() || cfg.isIncluirKpis()) {
                XSSFSheet hResumen = wb.createSheet("Resumen");
                escribirResumenExcel(hResumen, cfg, datos, estilos);
            }

            // ── Hoja login ───────────────────────────────────────────────
            if (conLogin && cfg.isIncluirDetalle() && datos.login != null && !datos.login.isEmpty()) {
                XSSFSheet hLogin = wb.createSheet("Accesos");
                escribirLoginExcel(hLogin, datos.login, cfg, estilos);
            }

            // ── Hoja cambios ─────────────────────────────────────────────
            if (conCambios && cfg.isIncluirDetalle() && datos.cambios != null && !datos.cambios.isEmpty()) {
                XSSFSheet hCambios = wb.createSheet("Cambios en datos");
                escribirCambiosExcel(hCambios, datos.cambios, cfg, estilos);
            }

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando Excel de auditoría", e);
        }
    }

    private void escribirResumenExcel(XSSFSheet sheet, ReporteAuditoriaConfigDTO cfg,
                                      DatosReporte datos, ExcelEstilos estilos) {
        int fila = 0;

        // Nombre del sistema / institución
        if (datos.institucion != null) {
            Institucion inst = datos.institucion;
            String nombreSist = (inst.getAppName() != null && !inst.getAppName().isBlank())
                    ? inst.getAppName() : (inst.getNombre() != null ? inst.getNombre() : "");
            if (!nombreSist.isBlank()) {
                Row rSist = sheet.createRow(fila++);
                Cell cSist = rSist.createCell(0);
                cSist.setCellValue(nombreSist);
                cSist.setCellStyle(estilos.titulo);
                sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 5));
            }
            if (inst.getAppName() != null && !inst.getAppName().isBlank()
                    && inst.getNombre() != null && !inst.getNombre().isBlank()) {
                Row rInst = sheet.createRow(fila++);
                Cell cInst = rInst.createCell(0);
                cInst.setCellValue(inst.getNombre());
                cInst.setCellStyle(estilos.subInfo);
                sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 5));
            }
        }

        // Título
        Row rTitulo = sheet.createRow(fila++);
        Cell cTitulo = rTitulo.createCell(0);
        cTitulo.setCellValue(cfg.getTitulo() != null ? cfg.getTitulo() : "Reporte de Auditoría");
        cTitulo.setCellStyle(estilos.titulo);
        sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 5));

        // Período
        Row rPeriodo = sheet.createRow(fila++);
        Cell cPeriodo = rPeriodo.createCell(0);
        cPeriodo.setCellValue("Período: "
                + (cfg.getDesde() != null ? cfg.getDesde() : "inicio")
                + " — "
                + (cfg.getHasta() != null ? cfg.getHasta() : "hoy"));
        cPeriodo.setCellStyle(estilos.subInfo);
        sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 5));
        fila++;

        if (datos.resumen == null) return;

        // KPIs accesos
        Row rH1 = sheet.createRow(fila++);
        Cell cH1 = rH1.createCell(0);
        cH1.setCellValue("ACCESOS AL SISTEMA");
        cH1.setCellStyle(estilos.seccion);
        sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 3));

        String[][] kpisLogin = {
                {"Total registros",  str(datos.resumen.get("login_total"))},
                {"Exitosos",         str(datos.resumen.get("login_exitosos"))},
                {"Fallidos",         str(datos.resumen.get("login_fallidos"))},
                {"Tasa de éxito",    str(datos.resumen.get("login_tasa")) + "%"},
        };
        for (String[] kpi : kpisLogin) {
            Row r = sheet.createRow(fila++);
            Cell cLabel = r.createCell(0); cLabel.setCellValue(kpi[0]); cLabel.setCellStyle(estilos.label);
            Cell cVal   = r.createCell(1); cVal.setCellValue(kpi[1]);   cVal.setCellStyle(estilos.valor);
        }
        fila++;

        // KPIs cambios
        Row rH2 = sheet.createRow(fila++);
        Cell cH2 = rH2.createCell(0);
        cH2.setCellValue("CAMBIOS EN DATOS");
        cH2.setCellStyle(estilos.seccion);
        sheet.addMergedRegion(new CellRangeAddress(fila-1, fila-1, 0, 3));

        String[][] kpisCambios = {
                {"Total cambios", str(datos.resumen.get("cambios_total"))},
                {"INSERT",        str(datos.resumen.get("cambios_insert"))},
                {"UPDATE",        str(datos.resumen.get("cambios_update"))},
                {"DELETE",        str(datos.resumen.get("cambios_delete"))},
        };
        for (String[] kpi : kpisCambios) {
            Row r = sheet.createRow(fila++);
            Cell cLabel = r.createCell(0); cLabel.setCellValue(kpi[0]); cLabel.setCellStyle(estilos.label);
            Cell cVal   = r.createCell(1); cVal.setCellValue(kpi[1]);   cVal.setCellStyle(estilos.valor);
        }

        // Generado por + fecha
        fila++;
        Row rGen = sheet.createRow(fila);
        Cell cGen = rGen.createCell(0);
        String textoGenExcel = "Generado: " + LocalDateTime.now().format(FMT_TS);
        if (cfg.getGeneradoPor() != null && !cfg.getGeneradoPor().isBlank()) {
            textoGenExcel += "  |  Por: " + cfg.getGeneradoPor();
        }
        cGen.setCellValue(textoGenExcel);
        cGen.setCellStyle(estilos.subInfo);

        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 4000);
    }

    private void escribirLoginExcel(XSSFSheet sheet, List<Map<String, Object>> rows,
                                    ReporteAuditoriaConfigDTO cfg, ExcelEstilos estilos) {
        String[] headers = {"ID", "Fecha", "Usuario App", "Usuario BD", "Resultado", "Motivo", "IP", "User Agent"};
        int fila = escribirEncabezadoExcel(sheet, headers, estilos);

        for (Map<String, Object> row : rows) {
            Row r = sheet.createRow(fila++);
            int col = 0;
            r.createCell(col++).setCellValue(str(row.get("id_aud")));
            r.createCell(col++).setCellValue(str(row.get("fecha")));
            r.createCell(col++).setCellValue(str(row.get("usuario_app")));
            r.createCell(col++).setCellValue(str(row.get("usuario_bd")));

            Cell cRes = r.createCell(col++);
            cRes.setCellValue(str(row.get("resultado")));
            String res = str(row.get("resultado"));
            if ("SUCCESS".equals(res)) cRes.setCellStyle(estilos.exitoso);
            if ("FAIL".equals(res))    cRes.setCellStyle(estilos.fallido);

            r.createCell(col++).setCellValue(str(row.get("motivo")));
            r.createCell(col++).setCellValue(str(row.get("ip_cliente")));
            r.createCell(col).setCellValue(str(row.get("user_agent")));
        }

        ajustarColumnas(sheet, headers.length);
        if (cfg.isExcelCongelarEncabezado()) sheet.createFreezePane(0, 1);
        if (cfg.isExcelFiltrosAutomaticos()) sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private void escribirCambiosExcel(XSSFSheet sheet, List<Map<String, Object>> rows,
                                      ReporteAuditoriaConfigDTO cfg, ExcelEstilos estilos) {
        String[] headers = {"ID", "Fecha", "Tabla", "ID Registro", "Operación", "Campo",
                "Valor Antes", "Valor Después", "Usuario App", "Usuario BD", "IP"};
        int fila = escribirEncabezadoExcel(sheet, headers, estilos);

        for (Map<String, Object> row : rows) {
            Row r = sheet.createRow(fila++);
            int col = 0;
            r.createCell(col++).setCellValue(str(row.get("id_aud_cambio")));
            r.createCell(col++).setCellValue(str(row.get("fecha")));
            r.createCell(col++).setCellValue(str(row.get("tabla")));
            r.createCell(col++).setCellValue(str(row.get("id_registro")));

            Cell cOp = r.createCell(col++);
            cOp.setCellValue(str(row.get("operacion")));
            String op = str(row.get("operacion"));
            if ("INSERT".equals(op)) cOp.setCellStyle(estilos.insert);
            if ("UPDATE".equals(op)) cOp.setCellStyle(estilos.update);
            if ("DELETE".equals(op)) cOp.setCellStyle(estilos.delete);

            r.createCell(col++).setCellValue(str(row.get("campo")));
            r.createCell(col++).setCellValue(str(row.get("valor_antes")));
            r.createCell(col++).setCellValue(str(row.get("valor_despues")));
            r.createCell(col++).setCellValue(str(row.get("usuario_app")));
            r.createCell(col++).setCellValue(str(row.get("usuario_bd")));
            r.createCell(col).setCellValue(str(row.get("ip_cliente")));
        }

        ajustarColumnas(sheet, headers.length);
        if (cfg.isExcelCongelarEncabezado()) sheet.createFreezePane(0, 1);
        if (cfg.isExcelFiltrosAutomaticos()) sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    private int escribirEncabezadoExcel(XSSFSheet sheet, String[] headers, ExcelEstilos estilos) {
        Row rHeader = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = rHeader.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(estilos.header);
        }
        return 1;
    }

    private void ajustarColumnas(XSSFSheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    // =========================================================================
    // UTILIDADES PDF
    // =========================================================================

    private PdfPTable crearTablaPdf(String[] headers, float[] widths, Color color)
            throws DocumentException {
        PdfPTable t = new PdfPTable(headers.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setSpacingBefore(6);

        Font fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fHeader));
            c.setBackgroundColor(color);
            c.setPadding(5);
            c.setBorderColor(color);
            t.addCell(c);
        }
        return t;
    }

    private PdfPCell celdaDato(String valor, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(valor != null ? valor : "—", f));
        c.setPadding(4);
        c.setBorderColor(new Color(229, 231, 235));
        return c;
    }

    private void agregarCeldaKpi(PdfPTable t, String label, String valor, Color color) {
        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font fValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, color);

        PdfPCell c = new PdfPCell();
        c.setPadding(10);
        c.setBorderColor(new Color(229, 231, 235));
        c.addElement(new Paragraph(label, fLabel));
        c.addElement(new Paragraph(valor, fValor));
        t.addCell(c);
    }

    // =========================================================================
    // ESTILOS EXCEL — clase interna
    // =========================================================================

    private static class ExcelEstilos {
        final XSSFCellStyle titulo, seccion, label, valor, subInfo, header;
        final XSSFCellStyle exitoso, fallido, insert, update, delete;

        ExcelEstilos(XSSFWorkbook wb, Color colorPrimario) {
            XSSFFont fTitulo = wb.createFont();
            fTitulo.setBold(true); fTitulo.setFontHeightInPoints((short) 16);
            fTitulo.setColor(new XSSFColor(colorPrimario, new DefaultIndexedColorMap()));

            XSSFFont fSeccion = wb.createFont();
            fSeccion.setBold(true); fSeccion.setFontHeightInPoints((short) 11);
            fSeccion.setColor(new XSSFColor(colorPrimario, new DefaultIndexedColorMap()));

            XSSFFont fHeader = wb.createFont();
            fHeader.setBold(true); fHeader.setColor(new XSSFColor(Color.WHITE, new DefaultIndexedColorMap()));

            titulo  = estilo(wb, fTitulo,  null,  false);
            seccion = estilo(wb, fSeccion, null,  false);
            label   = estilo(wb, null,     null,  false);
            valor   = estilo(wb, null,     null,  true);
            subInfo = estilo(wb, null,     null,  false);

            XSSFCellStyle hStyle = wb.createCellStyle();
            hStyle.setFont(fHeader);
            hStyle.setFillForegroundColor(new XSSFColor(colorPrimario, new DefaultIndexedColorMap()));
            hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hStyle.setAlignment(HorizontalAlignment.CENTER);
            header = hStyle;

            exitoso = colorCelda(wb, new Color(220, 252, 231));
            fallido = colorCelda(wb, new Color(254, 226, 226));
            insert  = colorCelda(wb, new Color(220, 252, 231));
            update  = colorCelda(wb, new Color(219, 234, 254));
            delete  = colorCelda(wb, new Color(254, 226, 226));
        }

        private static XSSFCellStyle estilo(XSSFWorkbook wb, XSSFFont font,
                                            Color bg, boolean bold) {
            XSSFCellStyle s = wb.createCellStyle();
            if (font != null) s.setFont(font);
            else if (bold) {
                XSSFFont f = wb.createFont(); f.setBold(true); s.setFont(f);
            }
            if (bg != null) {
                s.setFillForegroundColor(new XSSFColor(bg, new DefaultIndexedColorMap()));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return s;
        }

        private static XSSFCellStyle colorCelda(XSSFWorkbook wb, Color bg) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(new XSSFColor(bg, new DefaultIndexedColorMap()));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return s;
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static class DatosReporte {
        Institucion              institucion;
        Map<String, Object>      resumen;
        List<Map<String, Object>> login;
        List<Map<String, Object>> cambios;
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String str(Object o) {
        return o != null ? o.toString() : "—";
    }

    private String truncar(String s, int max) {
        if (s == null || s.equals("—")) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) return new Color(0, 166, 62);
        try {
            hex = hex.startsWith("#") ? hex.substring(1) : hex;
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return new Color(0, 166, 62);
        }
    }
}