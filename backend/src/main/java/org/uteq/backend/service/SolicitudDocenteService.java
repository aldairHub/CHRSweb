package org.uteq.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.entity.*;
import org.uteq.backend.repository.*;
import org.uteq.backend.dto.SolicitudDocenteRequestDTO;
import org.uteq.backend.dto.SolicitudDocenteResponseDTO;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SolicitudDocenteService {

    private final SolicitudDocenteRepository solicitudRepository;
    private final AutoridadAcademicaRepository autoridadRepository;
    private final CarreraRepository carreraRepository;
    private final MateriaRepository materiaRepository;
    private final AreaConocimientoRepository areaRepository;

    @Transactional
    public SolicitudDocenteResponseDTO crearSolicitud(SolicitudDocenteRequestDTO request, Long idAutoridad) {
        AutoridadAcademica autoridad = autoridadRepository.findById(idAutoridad)
                .orElseThrow(() -> new RuntimeException("Autoridad académica no encontrada"));
        Carrera carrera = carreraRepository.findById(request.getIdCarrera())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada"));
        Materia materia = materiaRepository.findById(request.getIdMateria())
                .orElseThrow(() -> new RuntimeException("Materia no encontrada"));
        AreaConocimiento area = areaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new RuntimeException("Área de conocimiento no encontrada"));

        SolicitudDocente solicitud = new SolicitudDocente();
        solicitud.setAutoridad(autoridad);
        solicitud.setCarrera(carrera);
        solicitud.setMateria(materia);
        solicitud.setArea(area);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstadoSolicitud("pendiente");
        solicitud.setJustificacion(request.getJustificacion());
        solicitud.setCantidadDocentes(request.getCantidadDocentes());
        solicitud.setNivelAcademico(request.getNivelAcademico());
        solicitud.setExperienciaProfesionalMin(request.getExperienciaProfesionalMin());
        solicitud.setExperienciaDocenteMin(request.getExperienciaDocenteMin());
        solicitud.setObservaciones(request.getObservaciones());

        SolicitudDocente savedSolicitud = solicitudRepository.save(solicitud);
        return convertToDTO(savedSolicitud);
    }

    public Long obtenerIdAutoridadPorUsuarioApp(String usuarioApp) {
        AutoridadAcademica autoridad = autoridadRepository.findByUsuario_UsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Autoridad académica no encontrada"));
        return autoridad.getIdAutoridad();
    }

    public List<SolicitudDocenteResponseDTO> obtenerTodasLasSolicitudes() {
        return solicitudRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public SolicitudDocenteResponseDTO obtenerSolicitudPorId(Long id) {
        SolicitudDocente solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        return convertToDTO(solicitud);
    }

    public byte[] generarPDFSolicitud(Long id) {
        try {
            String html = generarHTMLSolicitud(id);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage(), e);
        }
    }

    private String getLogoBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("static/imgs/logo-uteq.png");
            InputStream inputStream = resource.getInputStream();
            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();
            System.out.println("✅ Logo UTEQ cargado desde static/imgs/logo-uteq.png");
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            System.err.println("⚠️ Logo no encontrado, usando SVG fallback");
            return "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyYWQxIiB4MT0iMCUiIHkxPSIwJSIgeDI9IjEwMCUiIHkyPSIxMDAlIj48c3RvcCBvZmZzZXQ9IjAlIiBzdHlsZT0ic3RvcC1jb2xvcjojMDBEMTQ5O3N0b3Atb3BhY2l0eToxIi8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdHlsZT0ic3RvcC1jb2xvcjojMDBBNjNFO3N0b3Atb3BhY2l0eToxIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PGNpcmNsZSBjeD0iMzAiIGN5PSIzMCIgcj0iMjgiIGZpbGw9InVybCgjZ3JhZDEpIiBzdHJva2U9IiMwMTY2MzAiIHN0cm9rZS13aWR0aD0iMiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMjgiIGZvbnQtd2VpZ2h0PSJib2xkIiBmaWxsPSJ3aGl0ZSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zNWVtIj5VPC90ZXh0Pjwvc3ZnPg==";
        }
    }

    public String generarHTMLSolicitud(Long id) {
        SolicitudDocenteResponseDTO s = obtenerSolicitudPorId(id);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        String fechaFormateada = s.getFechaSolicitud() != null ? s.getFechaSolicitud().format(formatter) : "Fecha no disponible";
        String estadoColor = getEstadoColor(s.getEstadoSolicitud());
        String logoBase64 = getLogoBase64();

        return String.format("""
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>Solicitud</title><style type="text/css">
        @page{size:A4;margin:2cm}*{margin:0;padding:0}body{font-family:Arial,sans-serif;font-size:10pt;line-height:1.5;color:#1a1a1a}
        .header-container{margin-bottom:20px;padding-bottom:12px;border-bottom:3px solid #00A63E;page-break-inside:avoid}
        .header-content{display:table;width:100%%}.logo-cell{display:table-cell;width:70px;vertical-align:middle}
        .logo{width:55px;height:55px}.title-cell{display:table-cell;vertical-align:middle;padding-left:12px}
        .main-title{font-size:11pt;font-weight:bold;color:#016630;line-height:1.2;margin-bottom:2px}
        .subtitle{font-size:9pt;color:#536b50}.metadata-box{background-color:#EEF9EC;border:1px solid #B9F8CF;border-left:4px solid #00A63E;padding:12px 15px;margin-bottom:18px;page-break-inside:avoid}
        .meta-row{margin-bottom:5px;font-size:9pt}.meta-row:last-child{margin-bottom:0}
        .meta-label{display:inline-block;width:110px;font-weight:bold;color:#016630}.meta-value{color:#1a1a1a}
        .badge{display:inline-block;padding:3px 10px;border-radius:10px;font-size:8pt;font-weight:bold;text-transform:uppercase;color:white}
        .asunto-box{margin-bottom:12px;padding:10px 12px;background-color:#fafdfb;border-left:3px solid #00A63E;font-size:9.5pt;page-break-inside:avoid}
        .asunto-label{font-weight:bold;color:#016630}.saludo{margin:12px 0;font-style:italic;color:#536b50}
        .parrafo{text-align:justify;margin-bottom:18px;line-height:1.5}
        .section-title{font-size:10pt;font-weight:bold;color:#016630;margin:18px 0 12px 0;padding:6px 10px;background-color:#EEF9EC;border-left:3px solid #00A63E;page-break-after:avoid}
        .card{background-color:white;border:1px solid #d1d5db;margin-bottom:12px;page-break-inside:avoid}
        .card-header{background-color:#f8fdf9;padding:8px 12px;border-bottom:2px solid #B9F8CF}
        .card-title{font-weight:bold;color:#016630;font-size:9.5pt}.card-body{padding:12px}
        .info-table{width:100%%;border-collapse:collapse}.info-table tr{border-bottom:1px solid #e5e7eb}
        .info-table tr:last-child{border-bottom:none}.info-table td{padding:8px 10px;font-size:9pt}
        .info-table .label{font-weight:600;color:#536b50;width:42%%;background-color:#f9fafb}.info-table .value{color:#1a1a1a}
        .text-content{background-color:#f9fafb;border:1px solid #e5e7eb;padding:10px 12px;color:#1a1a1a;font-size:9pt;line-height:1.5}
        .cierre{margin-top:20px;font-style:italic;color:#536b50;page-break-inside:avoid}
        .firma-area{margin-top:50px;text-align:center;page-break-inside:avoid}
        .firma-linea{width:200px;margin:0 auto;padding-top:5px;border-top:2px solid #1a1a1a}
        .firma-nombre{font-weight:bold;font-size:10pt;margin-bottom:2px}.firma-cargo{font-size:9pt;color:#536b50}
        .footer-info{margin-top:30px;padding-top:10px;border-top:1px solid #d1d5db;text-align:center;font-size:8pt;color:#6b7280}
        </style></head><body>
        <div class="header-container"><div class="header-content"><div class="logo-cell"><img src="%s" alt="Logo" class="logo"/></div>
        <div class="title-cell"><div class="main-title">Normativa para la Selección y Contratación de Profesores No Titulares</div>
        <div class="subtitle">Universidad Técnica Estatal de Quevedo</div></div></div></div>
        <div class="metadata-box">
        <div class="meta-row"><span class="meta-label">ID Solicitud:</span><span class="meta-value">SOL-%05d</span></div>
        <div class="meta-row"><span class="meta-label">Fecha:</span><span class="meta-value">%s</span></div>
        <div class="meta-row"><span class="meta-label">Estado:</span><span class="badge" style="background-color:%s">%s</span></div>
        <div class="meta-row"><span class="meta-label">Solicitante:</span><span class="meta-value">%s</span></div></div>
        <div class="asunto-box"><span class="asunto-label">Asunto:</span> Solicitud de hojas de vida para contratación de profesor del área de conocimiento de %s.</div>
        <div class="saludo">De mi consideración:</div>
        <div class="parrafo">Con base en la propuesta de distributivo académico, se evidencia la necesidad de contratar docentes para cubrir los requerimientos académicos institucionales. A continuación se detallan las especificaciones del requerimiento:</div>
        <div class="section-title">Detalles del Requerimiento</div>
        <div class="card"><div class="card-header"><div class="card-title">Información Académica</div></div><div class="card-body"><table class="info-table">
        <tr><td class="label">Facultad</td><td class="value">%s</td></tr>
        <tr><td class="label">Carrera</td><td class="value">%s</td></tr>
        <tr><td class="label">Modalidad</td><td class="value">%s</td></tr>
        <tr><td class="label">Materia</td><td class="value">%s</td></tr>
        <tr><td class="label">Nivel</td><td class="value">%s</td></tr>
        <tr><td class="label">Área de Conocimiento</td><td class="value">%s</td></tr>
        </table></div></div>
        <div class="card"><div class="card-header"><div class="card-title">Requisitos del Docente</div></div><div class="card-body"><table class="info-table">
        <tr><td class="label">Cantidad de Docentes</td><td class="value">%d profesor(es)</td></tr>
        <tr><td class="label">Nivel Académico Requerido</td><td class="value">%s</td></tr>
        <tr><td class="label">Experiencia Profesional Mínima</td><td class="value">%d año(s)</td></tr>
        <tr><td class="label">Experiencia Docente Mínima</td><td class="value">%d año(s)</td></tr>
        </table></div></div>
        <div class="card"><div class="card-header"><div class="card-title">Justificación</div></div><div class="card-body"><div class="text-content">%s</div></div></div>
        <div class="card"><div class="card-header"><div class="card-title">Observaciones</div></div><div class="card-body"><div class="text-content">%s</div></div></div>
        <div class="cierre">Con sentimientos de distinguida consideración.</div>
        <div class="firma-area"><div class="firma-linea"><div class="firma-nombre">%s</div><div class="firma-cargo">Autoridad Académica</div></div></div>
        <div class="footer-info">Universidad Técnica Estatal de Quevedo - Sistema de Gestión Docente<br/>Documento generado el %s</div>
        </body></html>
        """,
                logoBase64, s.getIdSolicitud(), fechaFormateada, estadoColor,
                s.getEstadoSolicitud() != null ? s.getEstadoSolicitud().toUpperCase() : "PENDIENTE",
                s.getNombreAutoridad() != null ? s.getNombreAutoridad() : "No especificado",
                s.getNombreArea() != null ? s.getNombreArea().toUpperCase() : "NO ESPECIFICADA",
                s.getNombreFacultad() != null ? s.getNombreFacultad() : "No especificada",
                s.getNombreCarrera() != null ? s.getNombreCarrera() : "No especificada",
                s.getModalidadCarrera() != null ? s.getModalidadCarrera() : "No especificada",
                s.getNombreMateria() != null ? s.getNombreMateria() : "No especificada",
                s.getNivelMateria() != null ? s.getNivelMateria() : "N/A",
                s.getNombreArea() != null ? s.getNombreArea() : "No especificada",
                s.getCantidadDocentes(),
                s.getNivelAcademico() != null ? s.getNivelAcademico() : "No especificado",
                s.getExperienciaProfesionalMin(), s.getExperienciaDocenteMin(),
                s.getJustificacion() != null && !s.getJustificacion().isEmpty() ? s.getJustificacion() : "No se proporcionó justificación.",
                s.getObservaciones() != null && !s.getObservaciones().isEmpty() ? s.getObservaciones() : "Sin observaciones adicionales.",
                s.getNombreAutoridad() != null ? s.getNombreAutoridad() : "Autoridad Académica",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    private String getEstadoColor(String estado) {
        if (estado == null) return "#ea580c";
        return switch (estado.toLowerCase()) {
            case "aprobado" -> "#16a34a";
            case "rechazado" -> "#dc2626";
            default -> "#ea580c";
        };
    }

    private SolicitudDocenteResponseDTO convertToDTO(SolicitudDocente solicitud) {
        return SolicitudDocenteResponseDTO.builder()
                .idSolicitud(solicitud.getIdSolicitud())
                .idAutoridad(solicitud.getAutoridad().getIdAutoridad())
                .nombreAutoridad(solicitud.getAutoridad().getNombres() + " " + solicitud.getAutoridad().getApellidos())
                .idCarrera(solicitud.getCarrera().getIdCarrera())
                .nombreCarrera(solicitud.getCarrera().getNombreCarrera())
                .modalidadCarrera(solicitud.getCarrera().getModalidad())
                .idFacultad(solicitud.getCarrera().getFacultad().getIdFacultad())
                .nombreFacultad(solicitud.getCarrera().getFacultad().getNombreFacultad())
                .idMateria(solicitud.getMateria().getIdMateria())
                .nombreMateria(solicitud.getMateria().getNombreMateria())
                .nivelMateria(solicitud.getMateria().getNivel())
                .idArea(solicitud.getArea().getIdArea())
                .nombreArea(solicitud.getArea().getNombreArea())
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .estadoSolicitud(solicitud.getEstadoSolicitud())
                .justificacion(solicitud.getJustificacion())
                .cantidadDocentes(solicitud.getCantidadDocentes())
                .nivelAcademico(solicitud.getNivelAcademico())
                .experienciaProfesionalMin(solicitud.getExperienciaProfesionalMin())
                .experienciaDocenteMin(solicitud.getExperienciaDocenteMin())
                .observaciones(solicitud.getObservaciones())
                .build();
    }
}