package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.RequisitoPrepostulacionDTO;
import org.uteq.backend.dto.RequisitoPrepostulacionRequestDTO;
import org.uteq.backend.repository.PostgresProcedureRepository;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RequisitoPrepostulacionController {

    private final PostgresProcedureRepository proc;

    @GetMapping("/api/prepostulacion/solicitud/{idSolicitud}/requisitos")
    public ResponseEntity<List<RequisitoPrepostulacionDTO>> listarPublico(
            @PathVariable Long idSolicitud) {
        return ResponseEntity.ok(listar(idSolicitud));
    }

    @GetMapping("/api/admin/solicitudes/{idSolicitud}/requisitos")
    public ResponseEntity<List<RequisitoPrepostulacionDTO>> listarAdmin(
            @PathVariable Long idSolicitud) {
        return ResponseEntity.ok(listar(idSolicitud));
    }

    @PostMapping("/api/admin/solicitudes/{idSolicitud}/requisitos")
    public ResponseEntity<?> agregar(
            @PathVariable Long idSolicitud,
            @RequestBody RequisitoPrepostulacionRequestDTO req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El nombre es requerido"));

        Long id = proc.agregarRequisitoPrepostulacion(
                idSolicitud,
                req.getNombre().trim(),
                req.getDescripcion() != null ? req.getDescripcion().trim() : null,
                req.getOrden() != null ? req.getOrden() : 0);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RequisitoPrepostulacionDTO(id, req.getNombre(), req.getDescripcion(), req.getOrden()));
    }

    @PutMapping("/api/admin/solicitudes/requisitos/{idRequisito}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long idRequisito,
            @RequestBody RequisitoPrepostulacionRequestDTO req) {
        if (req.getNombre() == null || req.getNombre().isBlank())
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El nombre es requerido"));

        proc.actualizarRequisitoPrepostulacion(
                idRequisito,
                req.getNombre().trim(),
                req.getDescripcion() != null ? req.getDescripcion().trim() : null,
                req.getOrden() != null ? req.getOrden() : 0);

        return ResponseEntity.ok(Map.of("mensaje", "Requisito actualizado"));
    }

    @DeleteMapping("/api/admin/solicitudes/requisitos/{idRequisito}")
    public ResponseEntity<?> eliminar(@PathVariable Long idRequisito) {
        proc.eliminarRequisitoPrepostulacion(idRequisito);
        return ResponseEntity.ok(Map.of("mensaje", "Requisito eliminado"));
    }

    private List<RequisitoPrepostulacionDTO> listar(Long idSolicitud) {
        return proc.listarRequisitosSolicitud(idSolicitud).stream()
                .map(r -> new RequisitoPrepostulacionDTO(
                        toLong(r.get("id_requisito")),
                        (String) r.get("nombre"),
                        (String) r.get("descripcion"),
                        toInt(r.get("orden"))))
                .toList();
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        return o instanceof Long l ? l : ((Number) o).longValue();
    }

    private Integer toInt(Object o) {
        if (o == null) return 0;
        return o instanceof Integer i ? i : ((Number) o).intValue();
    }
}