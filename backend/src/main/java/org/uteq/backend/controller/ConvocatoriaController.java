package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ConvocatoriaDTO;
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

    private final ConvocatoriaRepository convocatoriaRepository;
    private final ConvocatoriaSolicitudRepository convocatoriaSolicitudRepository;
    private final SolicitudDocenteRepository solicitudDocenteRepository;

    // ─── PÚBLICO ────────────────────────────────────────────────────────────

    @GetMapping("/api/convocatorias/activas")
    public ResponseEntity<List<ConvocatoriaDTO>> listarAbiertas() {
        List<ConvocatoriaDTO> lista = convocatoriaRepository
                .findByEstadoConvocatoriaOrderByFechaPublicacionDesc("abierta")
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/api/convocatorias/{id}")
    public ResponseEntity<ConvocatoriaDTO> obtener(@PathVariable Long id) {
        return convocatoriaRepository.findById(id)
                .map(this::toDTO).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retorna las solicitudes_docente asociadas a una convocatoria.
     * El postulante usa esto para elegir a cuál plaza específica quiere aplicar.
     */
    @GetMapping("/api/convocatorias/{id}/solicitudes")
    public ResponseEntity<List<SolicitudDocenteDTO>> obtenerSolicitudes(@PathVariable Long id) {
        List<Long> idsSolicitud = convocatoriaSolicitudRepository
                .findByIdConvocatoria(id)
                .stream()
                .map(ConvocatoriaSolicitud::getIdSolicitud)
                .collect(Collectors.toList());

        if (idsSolicitud.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<SolicitudDocenteDTO> solicitudes = solicitudDocenteRepository
                .findAllById(idsSolicitud)
                .stream()
                .map(this::toSolicitudDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(solicitudes);
    }

    // ─── ADMIN ──────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/convocatorias")
    public ResponseEntity<List<ConvocatoriaDTO>> listarTodas() {
        List<ConvocatoriaDTO> lista = convocatoriaRepository
                .findAllByOrderByFechaPublicacionDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/api/admin/convocatorias")
    public ResponseEntity<ConvocatoriaDTO> crear(@RequestBody ConvocatoriaDTO dto) {
        Convocatoria c = fromDTO(dto);
        c.setFechaPublicacion(LocalDate.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(convocatoriaRepository.save(c)));
    }

    @PutMapping("/api/admin/convocatorias/{id}")
    public ResponseEntity<ConvocatoriaDTO> actualizar(@PathVariable Long id,
                                                      @RequestBody ConvocatoriaDTO dto) {
        return convocatoriaRepository.findById(id).map(existente -> {
            existente.setTitulo(dto.getTitulo());
            existente.setDescripcion(dto.getDescripcion());
            existente.setFechaInicio(dto.getFechaInicio());
            existente.setFechaFin(dto.getFechaFin());
            if (dto.getEstadoConvocatoria() != null)
                existente.setEstadoConvocatoria(dto.getEstadoConvocatoria());
            return ResponseEntity.ok(toDTO(convocatoriaRepository.save(existente)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/convocatorias/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!convocatoriaRepository.existsById(id)) return ResponseEntity.notFound().build();
        convocatoriaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Mappers ────────────────────────────────────────────────────────────

    private ConvocatoriaDTO toDTO(Convocatoria c) {
        ConvocatoriaDTO dto = new ConvocatoriaDTO();
        dto.setIdConvocatoria(c.getIdConvocatoria());
        dto.setTitulo(c.getTitulo());
        dto.setDescripcion(c.getDescripcion());
        dto.setFechaPublicacion(c.getFechaPublicacion());
        dto.setFechaInicio(c.getFechaInicio());
        dto.setFechaFin(c.getFechaFin());
        dto.setEstadoConvocatoria(c.getEstadoConvocatoria());
        return dto;
    }

    private Convocatoria fromDTO(ConvocatoriaDTO dto) {
        Convocatoria c = new Convocatoria();
        c.setTitulo(dto.getTitulo());
        c.setDescripcion(dto.getDescripcion());
        c.setFechaInicio(dto.getFechaInicio());
        c.setFechaFin(dto.getFechaFin());
        c.setEstadoConvocatoria(dto.getEstadoConvocatoria() != null
                ? dto.getEstadoConvocatoria() : "abierta");
        return c;
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