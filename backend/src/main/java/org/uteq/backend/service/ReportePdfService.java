package org.uteq.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.SolicitudDocente;

import java.io.ByteArrayOutputStream;

@Service
public class ReportePdfService {

    public byte[] generarReporte(SolicitudDocente solicitud) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // 🔥 CONTENIDO BÁSICO
            document.add(new Paragraph("Reporte de Solicitud Docente"));
            document.add(new Paragraph("ID: " + solicitud.getIdSolicitud()));
            document.add(new Paragraph("Estado: " + solicitud.getEstadoSolicitud()));
            document.add(new Paragraph("Cantidad docentes: " + solicitud.getCantidadDocentes()));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
