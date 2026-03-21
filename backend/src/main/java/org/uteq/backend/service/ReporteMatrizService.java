package org.uteq.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteMatrizService {

    private final JdbcTemplate jdbc;

    private static final Color COLOR_VERDE       = new Color(0, 100, 60);
    private static final Color COLOR_VERDE_CLARO = new Color(232, 245, 233);
    private static final Color COLOR_GRIS        = new Color(245, 245, 245);
    private static final Color COLOR_BORDE       = new Color(200, 200, 200);

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                    new java.util.Locale("es", "EC"));
    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH'h'mm");

    // ── Fuentes ────────────────────────────────────────────────────────────
    private Font fTitulo()    { return new Font(Font.HELVETICA, 13, Font.BOLD,   COLOR_VERDE); }
    private Font fSubtitulo() { return new Font(Font.HELVETICA, 11, Font.BOLD,   Color.BLACK); }
    private Font fNormal()    { return new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.BLACK); }
    private Font fNormalB()   { return new Font(Font.HELVETICA, 9,  Font.BOLD,   Color.BLACK); }
    private Font fPeq()       { return new Font(Font.HELVETICA, 8,  Font.NORMAL, Color.DARK_GRAY); }
    private Font fPeqB()      { return new Font(Font.HELVETICA, 8,  Font.BOLD,   Color.BLACK); }
    private Font fHeader()    { return new Font(Font.HELVETICA, 8,  Font.BOLD,   Color.WHITE); }

    // ══════════════════════════════════════════════════════════════════════
    // ACTA DE CALIFICACIÓN DE MÉRITOS
    // ══════════════════════════════════════════════════════════════════════
    public byte[] generarActa(Long idSolicitud) {
        Map<String, Object> datos = obtenerDatosSolicitud(idSolicitud);
        List<Map<String, Object>> candidatos = obtenerCandidatosConPuntajes(idSolicitud);
        List<Map<String, Object>> secciones  = obtenerSecciones();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PiePagina("ACTA DE CALIFICACIÓN DE MÉRITOS"));
            doc.open();

            agregarEncabezadoInstitucional(doc, datos, "ACTA DE CALIFICACIÓN DE MÉRITOS");
            doc.add(Chunk.NEWLINE);
            agregarParrafoIntroductorio(doc, datos, candidatos);
            doc.add(Chunk.NEWLINE);
            agregarTablaMatriz(doc, candidatos, secciones, false);
            doc.add(Chunk.NEWLINE);
            agregarFirmas(doc, datos);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando Acta PDF", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // INFORME FINAL DE SELECCIÓN
    // ══════════════════════════════════════════════════════════════════════
    public byte[] generarInformeFinal(Long idSolicitud) {
        Map<String, Object> datos     = obtenerDatosSolicitud(idSolicitud);
        List<Map<String, Object>> candidatos = obtenerCandidatosConPuntajes(idSolicitud);
        List<Map<String, Object>> secciones  = obtenerSecciones();
        Map<String, Object> decision  = obtenerDecision(idSolicitud);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PiePagina("INFORME DE SELECCIÓN DE PROFESOR NO TITULAR"));
            doc.open();

            agregarEncabezadoInstitucional(doc, datos, "INFORME DE SELECCIÓN DE PROFESOR NO TITULAR");
            doc.add(Chunk.NEWLINE);
            agregarSeccionNarrativa(doc, datos, candidatos);
            doc.add(Chunk.NEWLINE);
            agregarTablaMatriz(doc, candidatos, secciones, true);
            doc.add(Chunk.NEWLINE);
            agregarDecisionFinal(doc, decision, candidatos);
            doc.add(Chunk.NEWLINE);
            agregarFirmas(doc, datos);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando Informe Final PDF", e);
        }
    }

    // ── Encabezado institucional ────────────────────────────────────────
    private void agregarEncabezadoInstitucional(Document doc, Map<String, Object> datos,
                                                String tipoDoc) throws DocumentException {
        // Institución
        Paragraph inst = new Paragraph(
                str(datos.get("nombre_institucion"), "UNIVERSIDAD TÉCNICA ESTATAL DE QUEVEDO"),
                new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_VERDE));
        inst.setAlignment(Element.ALIGN_CENTER);
        doc.add(inst);

        Paragraph facultad = new Paragraph(
                str(datos.get("nombre_facultad"), "FACULTAD"),
                new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK));
        facultad.setAlignment(Element.ALIGN_CENTER);
        doc.add(facultad);

        doc.add(Chunk.NEWLINE);

        // Número y tipo de documento
        String numDoc = str(datos.get("codigo_solicitud"), "001-" + java.time.Year.now().getValue());
        Paragraph titulo = new Paragraph(tipoDoc + " " + numDoc, fTitulo());
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        // Línea separadora
        LineSeparator linea = new LineSeparator(1, 100, COLOR_VERDE, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(linea));
    }

    // ── Párrafo introductorio (Acta) ────────────────────────────────────
    private void agregarParrafoIntroductorio(Document doc, Map<String, Object> datos,
                                             List<Map<String, Object>> candidatos) throws DocumentException {
        LocalDateTime ahora = LocalDateTime.now();
        String ciudad  = str(datos.get("ciudad_institucion"), "Quevedo");
        String fecha   = ahora.format(FMT_FECHA);
        String hora    = ahora.format(FMT_HORA);
        String decano  = str(datos.get("nombre_autoridad"), "Decano/a de la Facultad");
        String cargo   = str(datos.get("cargo_autoridad"), "Decano/a");
        String carrera = str(datos.get("nombre_carrera"), "la Carrera");
        String materia = str(datos.get("nombre_materia"), "la asignatura");
        String area    = str(datos.get("nombre_area"), "el área de conocimiento");

        StringBuilder intro = new StringBuilder();
        intro.append("En la ciudad de ").append(ciudad).append(", a los ").append(ahora.getDayOfMonth())
                .append(" días del mes de ").append(ahora.getMonth().getDisplayName(
                        java.time.format.TextStyle.FULL, new java.util.Locale("es","EC")))
                .append(" de ").append(ahora.getYear()).append(", siendo las ").append(hora)
                .append(", se reúne el/la ").append(decano).append(", ").append(cargo)
                .append(", de conformidad con lo estipulado en los Artículos 17 y 18 de la Normativa ")
                .append("para la Selección y Contratación de Docentes No Titulares de la Universidad, ")
                .append("para calificar los documentos de los profesionales aspirantes a ser contratados ")
                .append("como profesores No Titulares para el área de conocimiento vinculada a ")
                .append(area).append(", a fin de ocupar la vacante disponible en ").append(materia)
                .append(" de la ").append(carrera).append(".");

        Paragraph p = new Paragraph(intro.toString(), fNormal());
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(8);
        doc.add(p);

        // Lista de candidatos
        Paragraph pCand = new Paragraph("Los profesionales evaluados son:", fNormalB());
        pCand.setSpacingAfter(4);
        doc.add(pCand);

        for (int i = 0; i < candidatos.size(); i++) {
            Map<String, Object> c = candidatos.get(i);
            String nombre  = str(c.get("apellidos"), "") + " " + str(c.get("nombres"), "");
            String titulos = str(c.get("titulos"), "");
            Paragraph item = new Paragraph(
                    (i+1) + ". " + nombre.toUpperCase() + (titulos.isBlank() ? "" : "; " + titulos),
                    fNormal());
            item.setIndentationLeft(20);
            doc.add(item);
        }
    }

    // ── Sección narrativa (Informe Final) ───────────────────────────────
    private void agregarSeccionNarrativa(Document doc, Map<String, Object> datos,
                                         List<Map<String, Object>> candidatos) throws DocumentException {
        Paragraph secTitulo = new Paragraph(
                "Selección mediante el uso de la base de datos", fSubtitulo());
        secTitulo.setSpacingBefore(6);
        secTitulo.setSpacingAfter(6);
        doc.add(secTitulo);

        String materia = str(datos.get("nombre_materia"), "la asignatura");
        String carrera = str(datos.get("nombre_carrera"), "la Carrera");
        Paragraph desc = new Paragraph(
                "El proceso de selección se fundamenta en la Normativa para la Selección y Contratación " +
                        "de Docentes No Titulares vigente. Se seleccionaron los perfiles idóneos para cubrir la " +
                        "vacante de " + materia + " de " + carrera + ", estableciéndose la siguiente terna:",
                fNormal());
        desc.setAlignment(Element.ALIGN_JUSTIFIED);
        desc.setSpacingAfter(6);
        doc.add(desc);

        for (int i = 0; i < candidatos.size(); i++) {
            Map<String, Object> c = candidatos.get(i);
            String nombre  = str(c.get("apellidos"), "") + " " + str(c.get("nombres"), "");
            String titulos = str(c.get("titulos"), "");
            Paragraph item = new Paragraph(
                    (i+1) + ". " + nombre.toUpperCase() + (titulos.isBlank() ? "" : "; " + titulos),
                    fNormal());
            item.setIndentationLeft(20);
            doc.add(item);
        }

        Paragraph sec2 = new Paragraph(
                "Evaluación basada en los parámetros: calificación de méritos y medidas de acción " +
                        "afirmativa y equidad de género", fSubtitulo());
        sec2.setSpacingBefore(10);
        sec2.setSpacingAfter(6);
        doc.add(sec2);
    }

    // ── Tabla de matriz ─────────────────────────────────────────────────
    private void agregarTablaMatriz(Document doc, List<Map<String, Object>> candidatos,
                                    List<Map<String, Object>> secciones,
                                    boolean incluirTotalesCompletos) throws DocumentException {

        int numCandidatos = candidatos.size();
        // Columnas: PARÁMETROS + N candidatos
        float[] anchos = new float[1 + numCandidatos];
        anchos[0] = 4f;
        for (int i = 1; i <= numCandidatos; i++) anchos[i] = 2f;

        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);

        // Fila de encabezado — PARÁMETROS
        PdfPCell hParam = celda("PARÁMETROS", fHeader(), COLOR_VERDE, Element.ALIGN_LEFT);
        hParam.setRowspan(2);
        tabla.addCell(hParam);

        // Encabezado candidatos
        for (int i = 0; i < numCandidatos; i++) {
            PdfPCell hCand = celda("CANDIDATO " + (i+1), fHeader(), COLOR_VERDE, Element.ALIGN_CENTER);
            tabla.addCell(hCand);
        }
        // Fila 2 encabezado — nombres
        for (Map<String, Object> c : candidatos) {
            String nombre = str(c.get("apellidos"), "") + "\n" + str(c.get("nombres"), "");
            PdfPCell nCand = celda(nombre, fPeqB(), COLOR_VERDE_CLARO, Element.ALIGN_CENTER);
            tabla.addCell(nCand);
        }

        // Filas por sección
        for (Map<String, Object> sec : secciones) {
            String tipo = str(sec.get("tipo"), "");
            if (!incluirTotalesCompletos && "entrevista".equals(tipo)) continue;

            // Fila de sección (header)
            String tituloSec = str(sec.get("titulo"), "") + " — MÁXIMO " +
                    sec.get("puntaje_maximo") + " PUNTOS";
            PdfPCell cSec = celda(tituloSec, fNormalB(), COLOR_GRIS, Element.ALIGN_LEFT);
            cSec.setColspan(1 + numCandidatos);
            tabla.addCell(cSec);

            // Items de la sección
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) sec.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String labelItem = str(item.get("label"), "");
                    if (item.get("puntos_por") != null) {
                        labelItem += " (" + item.get("puntos_por") + ")";
                    }
                    tabla.addCell(celda("  - " + labelItem, fPeq(), Color.WHITE, Element.ALIGN_LEFT));
                    for (Map<String, Object> cand : candidatos) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> puntajes = (Map<String, Object>) cand.get("puntajes");
                        String codigo = str(item.get("codigo"), "");
                        Object val = puntajes != null ? puntajes.get(codigo) : null;
                        String valStr = val != null && !"0".equals(val.toString()) ? val.toString() : "";
                        tabla.addCell(celda(valStr, fPeq(), Color.WHITE, Element.ALIGN_CENTER));
                    }
                }
            }

            // Subtotal sección
            tabla.addCell(celda("TOTAL " + str(sec.get("titulo"), "").toUpperCase(),
                    fNormalB(), COLOR_GRIS, Element.ALIGN_LEFT));
            for (Map<String, Object> cand : candidatos) {
                Object sub = cand.get("subtotal_" + str(sec.get("codigo"), "").toLowerCase());
                tabla.addCell(celda(sub != null ? sub.toString() : "—",
                        fNormalB(), COLOR_GRIS, Element.ALIGN_CENTER));
            }
        }

        // Acción afirmativa
        tabla.addCell(celda("ACCIÓN AFIRMATIVA (2 puntos c/u, máx. 4)", fNormalB(), COLOR_GRIS, Element.ALIGN_LEFT));
        for (Map<String, Object> cand : candidatos) {
            Object af = cand.get("total_accion_afirmativa");
            tabla.addCell(celda(af != null ? af.toString() : "0", fNormalB(), COLOR_GRIS, Element.ALIGN_CENTER));
        }

        // Totales finales
        if (incluirTotalesCompletos) {
            agregarFilaTotalFinal(tabla, "TOTAL MÉRITOS (50 pts)", candidatos, "total_merecimientos", numCandidatos);
            agregarFilaTotalFinal(tabla, "TOTAL EXPERIENCIA (25 pts)", candidatos, "total_experiencia", numCandidatos);
            agregarFilaTotalFinal(tabla, "TOTAL ENTREVISTA (25 pts)", candidatos, "total_entrevista", numCandidatos);
        }

        // TOTAL GENERAL
        PdfPCell cTotalLabel = celda("PUNTAJE TOTAL", fHeader(), COLOR_VERDE, Element.ALIGN_LEFT);
        tabla.addCell(cTotalLabel);
        for (Map<String, Object> cand : candidatos) {
            Object total = cand.get("puntaje_total");
            tabla.addCell(celda(total != null ? total.toString() : "—",
                    fHeader(), COLOR_VERDE, Element.ALIGN_CENTER));
        }

        doc.add(tabla);
    }

    private void agregarFilaTotalFinal(PdfPTable tabla, String label,
                                       List<Map<String, Object>> candidatos,
                                       String campo, int numCandidatos) {
        tabla.addCell(celda(label, fNormalB(), new Color(210, 235, 210), Element.ALIGN_LEFT));
        for (Map<String, Object> cand : candidatos) {
            Object val = cand.get(campo);
            tabla.addCell(celda(val != null ? val.toString() : "0",
                    fNormalB(), new Color(210, 235, 210), Element.ALIGN_CENTER));
        }
    }

    // ── Decisión final (solo Informe) ───────────────────────────────────
    private void agregarDecisionFinal(Document doc, Map<String, Object> decision,
                                      List<Map<String, Object>> candidatos) throws DocumentException {
        Paragraph sTitulo = new Paragraph("Decisión Final del Comité", fSubtitulo());
        sTitulo.setSpacingBefore(10);
        sTitulo.setSpacingAfter(6);
        doc.add(sTitulo);

        if (decision == null) {
            doc.add(new Paragraph("Sin decisión registrada.", fNormal()));
            return;
        }

        String ganador = str(decision.get("nombre_ganador"), "—");
        Object puntaje = decision.get("puntaje_final");
        String acta    = str(decision.get("acta_comite"), "");

        Paragraph pGanador = new Paragraph();
        pGanador.add(new Chunk("Candidato seleccionado: ", fNormalB()));
        pGanador.add(new Chunk(ganador.toUpperCase(), fNormal()));
        if (puntaje != null) {
            pGanador.add(new Chunk("  —  Puntaje final: " + puntaje + " pts", fNormal()));
        }
        pGanador.setSpacingAfter(6);
        doc.add(pGanador);

        if (!acta.isBlank()) {
            Paragraph pActa = new Paragraph();
            pActa.add(new Chunk("Observaciones del Comité:\n", fNormalB()));
            pActa.add(new Chunk(acta, fNormal()));
            pActa.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(pActa);
        }

        // Nota normativa
        doc.add(Chunk.NEWLINE);
        Paragraph nota = new Paragraph(
                "Según lo señalado en el Art. 7 de la Normativa para la Selección y Contratación de " +
                        "Docentes No Titulares, se procede a notificar el resultado de este proceso al " +
                        "Consejo Directivo para el trámite respectivo.", fPeq());
        nota.setAlignment(Element.ALIGN_JUSTIFIED);
        doc.add(nota);
    }

    // ── Firmas ─────────────────────────────────────────────────────────
    private void agregarFirmas(Document doc, Map<String, Object> datos) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        PdfPTable tablaFirmas = new PdfPTable(3);
        tablaFirmas.setWidthPercentage(80);
        tablaFirmas.setHorizontalAlignment(Element.ALIGN_CENTER);

        String autoridad = str(datos.get("nombre_autoridad"), "Decano/a");
        String cargo     = str(datos.get("cargo_autoridad"), "Decano/a de la Facultad");

        // Firma izquierda
        PdfPCell f1 = new PdfPCell();
        f1.setBorder(Rectangle.NO_BORDER);
        f1.addElement(new Paragraph("\n\n_______________________________", fNormal()));
        f1.addElement(new Paragraph(autoridad, fNormalB()));
        f1.addElement(new Paragraph(cargo, fPeq()));
        tabla(tablaFirmas, f1);

        // Espacio central
        PdfPCell fEsp = new PdfPCell();
        fEsp.setBorder(Rectangle.NO_BORDER);
        tabla(tablaFirmas, fEsp);

        // Firma derecha
        PdfPCell f2 = new PdfPCell();
        f2.setBorder(Rectangle.NO_BORDER);
        f2.addElement(new Paragraph("\n\n_______________________________", fNormal()));
        f2.addElement(new Paragraph("Coordinador/a de Carrera", fNormalB()));
        f2.addElement(new Paragraph(str(datos.get("nombre_carrera"), ""), fPeq()));
        tabla(tablaFirmas, f2);

        doc.add(tablaFirmas);
        doc.add(Chunk.NEWLINE);
        Paragraph nota = new Paragraph("Nota: Se adjuntan las evidencias de todo el proceso.", fPeq());
        nota.setAlignment(Element.ALIGN_CENTER);
        doc.add(nota);
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private PdfPCell celda(String texto, Font font, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(COLOR_BORDE);
        c.setBorderWidth(0.5f);
        return c;
    }

    private void tabla(PdfPTable t, PdfPCell c) { t.addCell(c); }

    private String str(Object val, String fallback) {
        if (val == null) return fallback;
        String s = val.toString().trim();
        return s.isBlank() ? fallback : s;
    }

    // ── Consultas BD ────────────────────────────────────────────────────
    private Map<String, Object> obtenerDatosSolicitud(Long idSolicitud) {
        return jdbc.queryForMap("""
            SELECT
                COALESCE(i.nombre, 'Universidad Técnica Estatal de Quevedo') AS nombre_institucion,
                COALESCE(i.nombre_corto, '')                                  AS ciudad_institucion,
                f.nombre_facultad,
                c.nombre_carrera,
                m.nombre_materia,
                ac.nombre_area,
                sd.id_solicitud::TEXT                                         AS codigo_solicitud,
                aa.nombres || ' ' || aa.apellidos                            AS nombre_autoridad,
                'Autoridad Académica'                                         AS cargo_autoridad
            FROM solicitud_docente sd
            JOIN materia m              ON sd.id_materia   = m.id_materia
            JOIN area_conocimiento ac   ON sd.id_area      = ac.id_area
            JOIN carrera c              ON sd.id_carrera   = c.id_carrera
            JOIN facultad f             ON c.id_facultad   = f.id_facultad
            JOIN autoridad_academica aa ON sd.id_autoridad = aa.id_autoridad
            LEFT JOIN institucion i     ON i.activo = TRUE
            WHERE sd.id_solicitud = ?
            LIMIT 1
            """, idSolicitud);
    }

    private List<Map<String, Object>> obtenerCandidatosConPuntajes(Long idSolicitud) {
        List<Map<String, Object>> candidatos = jdbc.queryForList("""
            SELECT
                pe.id_proceso,
                p.id_postulante,
                p.nombres_postulante  AS nombres,
                p.apellidos_postulante AS apellidos,
                pre.nombres            AS titulos,
                COALESCE(pe.puntaje_matriz, 0)              AS puntaje_matriz,
                COALESCE(pe.puntaje_entrevista, 0)          AS puntaje_entrevista,
                COALESCE(pe.puntaje_matriz, 0) +
                  COALESCE(pe.puntaje_entrevista, 0)        AS puntaje_total,
                pe.decision_comite,
                pe.acta_comite
            FROM proceso_evaluacion pe
            JOIN postulante p        ON pe.id_postulante     = p.id_postulante
            JOIN prepostulacion pre  ON p.id_prepostulacion  = pre.id_prepostulacion
            WHERE pe.id_solicitud = ?
            ORDER BY puntaje_total DESC
            """, idSolicitud);

        // Enriquecer con puntajes por ítem y subtotales
        for (Map<String, Object> cand : candidatos) {
            Long idProceso = ((Number) cand.get("id_proceso")).longValue();
            List<Map<String, Object>> items = jdbc.queryForList(
                    "SELECT item_id, valor FROM matriz_meritos_puntaje WHERE id_proceso = ?",
                    idProceso);
            java.util.Map<String, Object> puntajes = new java.util.HashMap<>();
            for (Map<String, Object> it : items) {
                puntajes.put(str(it.get("item_id"), ""), it.get("valor"));
            }
            cand.put("puntajes", puntajes);
            // Totales calculados desde proceso_evaluacion
            cand.put("total_merecimientos",       cand.get("puntaje_matriz"));
            cand.put("total_experiencia",          0);
            cand.put("total_entrevista",           cand.get("puntaje_entrevista"));
            cand.put("total_accion_afirmativa",    0);
        }
        return candidatos;
    }

    private List<Map<String, Object>> obtenerSecciones() {
        List<Map<String, Object>> secciones = jdbc.queryForList("""
            SELECT id_seccion, codigo, titulo, descripcion, puntaje_maximo, orden, tipo
            FROM matriz_seccion WHERE activo = TRUE ORDER BY orden
            """);
        for (Map<String, Object> sec : secciones) {
            Long idSec = ((Number) sec.get("id_seccion")).longValue();
            List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT codigo, label, puntaje_maximo, puntos_por, orden
                FROM matriz_item WHERE id_seccion = ? AND activo = TRUE ORDER BY orden
                """, idSec);
            sec.put("items", items);
        }
        return secciones;
    }

    private Map<String, Object> obtenerDecision(Long idSolicitud) {
        try {
            return jdbc.queryForMap("""
                SELECT nombre_ganador, puntaje_final, acta_comite, fecha_envio
                FROM decision_revisor WHERE id_solicitud = ?
                ORDER BY fecha_envio DESC LIMIT 1
                """, idSolicitud);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Pie de página ────────────────────────────────────────────────────
    static class PiePagina extends PdfPageEventHelper {
        private final String titulo;
        PiePagina(String titulo) { this.titulo = titulo; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font f = new Font(Font.HELVETICA, 7, Font.ITALIC, Color.GRAY);
            Phrase pie = new Phrase(titulo + "  —  Pág. " + writer.getPageNumber(), f);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pie,
                    (document.left() + document.right()) / 2,
                    document.bottom() - 10, 0);
        }
    }
}