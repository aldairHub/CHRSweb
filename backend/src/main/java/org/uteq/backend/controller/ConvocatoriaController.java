package org.uteq.backend.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ConvocatoriaDTO.*;
import org.uteq.backend.dto.SolicitudDocenteDTO;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.entity.ConvocatoriaSolicitud;
import org.uteq.backend.entity.SolicitudDocente;
import org.uteq.backend.repository.ConvocatoriaRepository;
import org.uteq.backend.repository.ConvocatoriaSolicitudRepository;
import org.uteq.backend.repository.SolicitudDocenteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConvocatoriaController {

    private final ConvocatoriaRepository          convocatoriaRepository;
    private final ConvocatoriaSolicitudRepository convocatoriaSolicitudRepository;
    private final SolicitudDocenteRepository      solicitudDocenteRepository;

    // ─── PÚBLICO ────────────────────────────────────────────────────────────

    @GetMapping("/api/convocatorias/activas")
    @Transactional
    public ResponseEntity<List<ListaResponse>> listarAbiertas() {
        List<ListaResponse> lista = convocatoriaRepository
                .findByEstadoConvocatoriaOrderByFechaPublicacionDesc("abierta")
                .stream().map(this::toListaResponse).collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/api/convocatorias/{id}")
    @Transactional
    public ResponseEntity<DetalleResponse> obtener(@PathVariable Long id) {
        return convocatoriaRepository.findById(id)
                .map(this::toDetalleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/convocatorias/{id}/solicitudes")
    @Transactional
    public ResponseEntity<List<SolicitudDocenteDTO>> obtenerSolicitudes(@PathVariable Long id) {
        List<Long> ids = convocatoriaSolicitudRepository
                .findByIdConvocatoria(id)
                .stream().map(ConvocatoriaSolicitud::getIdSolicitud)
                .collect(Collectors.toList());

        if (ids.isEmpty()) return ResponseEntity.ok(List.of());

        List<SolicitudDocenteDTO> solicitudes = solicitudDocenteRepository
                .findAllById(ids)
                .stream().map(this::toSolicitudDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(solicitudes);
    }

    // ─── ADMIN ──────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/convocatorias")
    @Transactional
    public ResponseEntity<List<ListaResponse>> listarTodas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String titulo
    ) {
        List<Convocatoria> todas = convocatoriaRepository.findAllByOrderByFechaPublicacionDesc();

        if (estado != null && !estado.isBlank()) {
            todas = todas.stream()
                    .filter(c -> estado.equalsIgnoreCase(c.getEstadoConvocatoria()))
                    .collect(Collectors.toList());
        }
        if (titulo != null && !titulo.isBlank()) {
            String filtro = titulo.toLowerCase();
            todas = todas.stream()
                    .filter(c -> c.getTitulo().toLowerCase().contains(filtro))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(todas.stream().map(this::toListaResponse).collect(Collectors.toList()));
    }

    @GetMapping("/api/admin/convocatorias/{id}")
    @Transactional
    public ResponseEntity<DetalleResponse> detalle(@PathVariable Long id) {
        return convocatoriaRepository.findById(id)
                .map(this::toDetalleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/convocatorias")
    @Transactional  // ← AGREGADO: sin esto el save de ConvocatoriaSolicitud falla
    public ResponseEntity<MensajeResponse> crear(@RequestBody CrearRequest req) {
        try {
            Convocatoria c = new Convocatoria();
            c.setTitulo(req.getTitulo());
            c.setDescripcion(req.getDescripcion());
            c.setFechaPublicacion(req.getFechaPublicacion() != null ? req.getFechaPublicacion() : LocalDate.now());
            c.setFechaInicio(req.getFechaInicio());
            c.setFechaFin(req.getFechaFin());
            c.setEstadoConvocatoria("abierta");
            Convocatoria saved = convocatoriaRepository.save(c);

            if (req.getIdsSolicitudes() != null && !req.getIdsSolicitudes().isEmpty()) {
                for (Long idSolicitud : req.getIdsSolicitudes()) {
                    ConvocatoriaSolicitud cs = new ConvocatoriaSolicitud();
                    cs.setIdConvocatoria(saved.getIdConvocatoria());
                    cs.setIdSolicitud(idSolicitud);
                    convocatoriaSolicitudRepository.save(cs);
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MensajeResponse(true, "Convocatoria creada correctamente", saved.getIdConvocatoria()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MensajeResponse(false, "Error al crear la convocatoria: " + e.getMessage(), null));
        }
    }

    @PutMapping("/api/admin/convocatorias/{id}")
    @Transactional
    public ResponseEntity<MensajeResponse> actualizar(
            @PathVariable Long id,
            @RequestBody ActualizarRequest req
    ) {
        return convocatoriaRepository.findById(id).map(c -> {
            if (!"abierta".equalsIgnoreCase(c.getEstadoConvocatoria())) {
                return ResponseEntity.badRequest()
                        .body(new MensajeResponse(false, "Solo se pueden editar convocatorias abiertas", null));
            }
            c.setTitulo(req.getTitulo());
            c.setDescripcion(req.getDescripcion());
            if (req.getFechaPublicacion() != null) c.setFechaPublicacion(req.getFechaPublicacion());
            c.setFechaInicio(req.getFechaInicio());
            c.setFechaFin(req.getFechaFin());
            convocatoriaRepository.save(c);
            return ResponseEntity.ok(new MensajeResponse(true, "Convocatoria actualizada correctamente", null));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MensajeResponse(false, "Convocatoria no encontrada", null)));
    }

    @PatchMapping("/api/admin/convocatorias/{id}/estado")
    @Transactional
    public ResponseEntity<MensajeResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestBody CambiarEstadoRequest req
    ) {
        return convocatoriaRepository.findById(id).map(c -> {
            String nuevoEstado = req.getNuevoEstado();
            List<String> estadosValidos = List.of("abierta", "cerrada", "cancelada");
            if (!estadosValidos.contains(nuevoEstado)) {
                return ResponseEntity.badRequest()
                        .body(new MensajeResponse(false, "Estado inválido. Use: abierta, cerrada o cancelada", null));
            }
            c.setEstadoConvocatoria(nuevoEstado);
            convocatoriaRepository.save(c);
            return ResponseEntity.ok(new MensajeResponse(true, "Estado actualizado a: " + nuevoEstado, null));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MensajeResponse(false, "Convocatoria no encontrada", null)));
    }

    @DeleteMapping("/api/admin/convocatorias/{id}")  // ← paréntesis corregido
    @Transactional
    public ResponseEntity<MensajeResponse> eliminar(@PathVariable Long id) {
        if (!convocatoriaRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MensajeResponse(false, "Convocatoria no encontrada", null));
        }
        convocatoriaSolicitudRepository.deleteByIdConvocatoria(id);
        convocatoriaRepository.deleteById(id);
        return ResponseEntity.ok(new MensajeResponse(true, "Convocatoria eliminada correctamente", null));
    }

    // ─── Mappers ────────────────────────────────────────────────────────────

    private ListaResponse toListaResponse(Convocatoria c) {
        long totalSolicitudes = convocatoriaSolicitudRepository.countByIdConvocatoria(c.getIdConvocatoria());
        return ListaResponse.builder()
                .idConvocatoria(c.getIdConvocatoria())
                .titulo(c.getTitulo())
                .descripcion(c.getDescripcion())
                .fechaPublicacion(c.getFechaPublicacion())
                .fechaInicio(c.getFechaInicio())
                .fechaFin(c.getFechaFin())
                .estadoConvocatoria(c.getEstadoConvocatoria())
                .totalSolicitudes(totalSolicitudes)
                .build();
    }

    private DetalleResponse toDetalleResponse(Convocatoria c) {
        List<Long> ids = convocatoriaSolicitudRepository
                .findByIdConvocatoria(c.getIdConvocatoria())
                .stream().map(ConvocatoriaSolicitud::getIdSolicitud)
                .collect(Collectors.toList());

        List<SolicitudResumen> solicitudes = ids.isEmpty() ? List.of() :
                solicitudDocenteRepository.findAllById(ids)
                        .stream().map(this::toSolicitudResumen)
                        .collect(Collectors.toList());

        return DetalleResponse.builder()
                .idConvocatoria(c.getIdConvocatoria())
                .titulo(c.getTitulo())
                .descripcion(c.getDescripcion())
                .fechaPublicacion(c.getFechaPublicacion())
                .fechaInicio(c.getFechaInicio())
                .fechaFin(c.getFechaFin())
                .estadoConvocatoria(c.getEstadoConvocatoria())
                .solicitudes(solicitudes)
                .build();
    }

    private SolicitudResumen toSolicitudResumen(SolicitudDocente s) {
        String nombreMateria  = s.getMateria()  != null ? s.getMateria().getNombreMateria()  : "";
        String nombreCarrera  = s.getCarrera()  != null ? s.getCarrera().getNombreCarrera()  : "";
        String nombreFacultad = (s.getCarrera() != null && s.getCarrera().getFacultad() != null)
                ? s.getCarrera().getFacultad().getNombreFacultad() : "";

        return SolicitudResumen.builder()
                .idSolicitud(s.getIdSolicitud())
                .nombreMateria(nombreMateria)
                .nombreCarrera(nombreCarrera)
                .nombreFacultad(nombreFacultad)
                .cantidadDocentes(s.getCantidadDocentes())
                .nivelAcademico(s.getNivelAcademico())
                .estadoSolicitud(s.getEstadoSolicitud())
                .build();
    }

    private SolicitudDocenteDTO toSolicitudDTO(SolicitudDocente s) {
        SolicitudDocenteDTO dto = new SolicitudDocenteDTO();
        dto.setIdSolicitud(s.getIdSolicitud());
        dto.setJustificacion(s.getJustificacion());
        dto.setCantidadDocentes(s.getCantidadDocentes());
        dto.setNivelAcademico(s.getNivelAcademico());
        dto.setEstadoSolicitud(s.getEstadoSolicitud());
        dto.setIdMateria(s.getMateria().getIdMateria());
        dto.setNombreMateria(s.getMateria().getNombreMateria());
        dto.setIdCarrera(s.getCarrera().getIdCarrera());
        dto.setNombreCarrera(s.getCarrera().getNombreCarrera());
        dto.setIdArea(s.getArea().getIdArea());
        dto.setNombreArea(s.getArea().getNombreArea());
        return dto;
    }
}