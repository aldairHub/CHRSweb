package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.DocumentoAcademicoDTO;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.entity.Prepostulacion;
import org.uteq.backend.entity.SolicitudDocente;
import org.uteq.backend.repository.*;
import org.uteq.backend.service.PrepostulacionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/prepostulaciones")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RequiredArgsConstructor
public class AdminPrepostulacionController {

    private final PrepostulacionService prepostulacionService;
    private final PrepostulacionDocumentoRepository documentoRepository;
    private final PrepostulacionSolicitudRepository prepostulacionSolicitudRepository;
    private final ConvocatoriaSolicitudRepository convocatoriaSolicitudRepository;
    private final SolicitudDocenteRepository solicitudDocenteRepository;
    private final ConvocatoriaRepository convocatoriaRepository;

    // ─── LISTAR TODAS ───────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarPrepostulaciones() {
        List<Prepostulacion> prepostulaciones = prepostulacionService.listarTodas();

        List<Map<String, Object>> result = prepostulaciones.stream().map(p -> {

            // idSolicitud desde tabla pivote (EmbeddedId → id.idSolicitud)
            List<Long> idsSolicitud = prepostulacionSolicitudRepository
                    .findByIdIdPrepostulacion(p.getIdPrepostulacion())
                    .stream()
                    .map(ps -> ps.getId().getIdSolicitud())
                    .collect(Collectors.toList());

            Long idSolicitud = idsSolicitud.isEmpty() ? null : idsSolicitud.get(0);

            // idConvocatoria desde convocatoria_solicitud
            Long idConvocatoria = null;
            if (idSolicitud != null) {
                List<Long> idsConv = convocatoriaSolicitudRepository
                        .findByIdSolicitud(idSolicitud)
                        .stream()
                        .map(cs -> cs.getIdConvocatoria())
                        .collect(Collectors.toList());
                idConvocatoria = idsConv.isEmpty() ? null : idsConv.get(0);
            }

            HashMap<String, Object> map = new HashMap<>();
            map.put("idPrepostulacion", p.getIdPrepostulacion());
            map.put("nombres",          p.getNombres());
            map.put("apellidos",        p.getApellidos());
            map.put("identificacion",   p.getIdentificacion());
            map.put("correo",           p.getCorreo());
            map.put("estadoRevision",   p.getEstadoRevision());
            map.put("fechaEnvio",       p.getFechaEnvio());
            map.put("urlCedula",        p.getUrlCedula());
            map.put("urlFoto",          p.getUrlFoto());
            map.put("idSolicitud",      idSolicitud);
            map.put("idConvocatoria",   idConvocatoria);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─── LISTAR POR ESTADO ──────────────────────────────────────────────────
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Prepostulacion>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(prepostulacionService.listarPorEstado(estado));
    }

    // ─── OBTENER UNA ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Prepostulacion> obtenerPrepostulacion(@PathVariable Long id) {
        return ResponseEntity.ok(prepostulacionService.obtenerPorId(id));
    }

    // ─── DOCUMENTOS ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> obtenerDocumentos(@PathVariable Long id) {
        Prepostulacion p = prepostulacionService.obtenerPorId(id);

        List<DocumentoAcademicoDTO> docs = documentoRepository
                .findByIdPrepostulacion(id)
                .stream()
                .map(d -> new DocumentoAcademicoDTO(
                        d.getIdDocumento(),
                        d.getDescripcion(),
                        d.getUrlDocumento(),
                        d.getFechaSubida()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "cedula",               p.getUrlCedula()  != null ? p.getUrlCedula()  : "",
                "foto",                 p.getUrlFoto()    != null ? p.getUrlFoto()    : "",
                "documentosAcademicos", docs,
                "nombreCompleto",       p.getNombres() + " " + p.getApellidos(),
                "identificacion",       p.getIdentificacion()
        ));
    }

    // ─── ACTUALIZAR ESTADO ──────────────────────────────────────────────────
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        String nuevoEstado   = (String) body.get("estado");
        String observaciones = (String) body.get("observaciones");
        Long idRevisor = body.get("idRevisor") != null
                ? ((Number) body.get("idRevisor")).longValue() : null;

        prepostulacionService.actualizarEstado(id, nuevoEstado, observaciones, idRevisor);

        return ResponseEntity.ok(Map.of(
                "mensaje",      "Estado actualizado correctamente",
                "nuevoEstado",  nuevoEstado
        ));
    }

    // ─── DETALLE (convocatoria + solicitud) ─────────────────────────────────
    @GetMapping("/{id}/detalle")
    public ResponseEntity<?> obtenerDetalle(@PathVariable Long id) {
        try {
            // 1. idSolicitud desde tabla pivote
            List<Long> idsSolicitud = prepostulacionSolicitudRepository
                    .findByIdIdPrepostulacion(id)
                    .stream()
                    .map(ps -> ps.getId().getIdSolicitud())
                    .collect(Collectors.toList());

            if (idsSolicitud.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Esta prepostulación no tiene solicitud asociada"));
            }

            Long idSolicitud = idsSolicitud.get(0);

            // 2. Solicitud docente
            SolicitudDocente solicitud = solicitudDocenteRepository
                    .findById(idSolicitud)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

            // 3. Convocatoria via convocatoria_solicitud
            List<Long> idsConvocatoria = convocatoriaSolicitudRepository
                    .findByIdSolicitud(idSolicitud)
                    .stream()
                    .map(cs -> cs.getIdConvocatoria())
                    .collect(Collectors.toList());

            if (idsConvocatoria.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "mensaje", "Esta solicitud no tiene convocatoria asociada"));
            }

            Convocatoria convocatoria = convocatoriaRepository
                    .findById(idsConvocatoria.get(0))
                    .orElseThrow(() -> new RuntimeException("Convocatoria no encontrada"));

            // 4. Respuesta
            Map<String, Object> solicitudMap = new HashMap<>();
            solicitudMap.put("idSolicitud", solicitud.getIdSolicitud());
            solicitudMap.put("nivelAcademico", solicitud.getNivelAcademico());
            solicitudMap.put("experienciaDocenteMin", solicitud.getExperienciaDocenteMin());
            solicitudMap.put("experienciaProfesionalMin", solicitud.getExperienciaProfesionalMin());
            solicitudMap.put("cantidadDocentes", solicitud.getCantidadDocentes());
            solicitudMap.put("justificacion", solicitud.getJustificacion() != null ? solicitud.getJustificacion() : "");
            solicitudMap.put("estadoSolicitud", solicitud.getEstadoSolicitud());
            solicitudMap.put("fechaSolicitud", solicitud.getFechaSolicitud() != null ? solicitud.getFechaSolicitud().toString() : "");

            Map<String, Object> convocatoriaMap = new HashMap<>();
            convocatoriaMap.put("idConvocatoria", convocatoria.getIdConvocatoria());
            convocatoriaMap.put("titulo", convocatoria.getTitulo());
            convocatoriaMap.put("descripcion", convocatoria.getDescripcion() != null ? convocatoria.getDescripcion() : "");
            convocatoriaMap.put("estadoConvocatoria", convocatoria.getEstadoConvocatoria());
            convocatoriaMap.put("fechaInicio", convocatoria.getFechaInicio()     != null ? convocatoria.getFechaInicio().toString()     : "");
            convocatoriaMap.put("fechaFin", convocatoria.getFechaFin()        != null ? convocatoria.getFechaFin().toString()        : "");
            convocatoriaMap.put("fechaPublicacion", convocatoria.getFechaPublicacion() != null ? convocatoria.getFechaPublicacion().toString() : "");

            return ResponseEntity.ok(Map.of(
                    "solicitud", solicitudMap,
                    "convocatoria", convocatoriaMap
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("mensaje", e.getMessage()));
        }
    }
}