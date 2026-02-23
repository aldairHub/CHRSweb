package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ConvocatoriaDTO;
import org.uteq.backend.entity.Convocatoria;
import org.uteq.backend.repository.ConvocatoriaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ConvocatoriaController {

    private final ConvocatoriaRepository convocatoriaRepository;

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
}