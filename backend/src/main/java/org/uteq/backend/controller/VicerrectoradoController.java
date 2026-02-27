package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.Prepostulacion;
import org.uteq.backend.service.PrepostulacionService;
import org.uteq.backend.dto.ActualizarEstadoDTO;
import org.uteq.backend.dto.PrepostulacionDetalleDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vicerrectorado")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VicerrectoradoController {

    private final PrepostulacionService prepostulacionService;

    @GetMapping("/prepostulaciones")
    public ResponseEntity<List<Prepostulacion>> listarPrepostulaciones() {
        List<Prepostulacion> prepostulaciones = prepostulacionService.listarTodas();
        return ResponseEntity.ok(prepostulaciones);
    }

    @GetMapping("/prepostulaciones/estado/{estado}")
    public ResponseEntity<List<Prepostulacion>> listarPorEstado(@PathVariable String estado) {
        List<Prepostulacion> prepostulaciones = prepostulacionService.listarPorEstado(estado);
        return ResponseEntity.ok(prepostulaciones);
    }

    @GetMapping("/prepostulaciones/{id}")
    public ResponseEntity<Prepostulacion> obtenerPrepostulacion(@PathVariable Long id) {
        Prepostulacion prepostulacion = prepostulacionService.obtenerPorId(id);
        return ResponseEntity.ok(prepostulacion);
    }

    @GetMapping("/prepostulaciones/{id}/documentos")
    public ResponseEntity<Map<String, String>> obtenerDocumentos(@PathVariable Long id) {
        Prepostulacion p = prepostulacionService.obtenerPorId(id);
        return ResponseEntity.ok(Map.of(
                "cedula", p.getUrlCedula() != null ? p.getUrlCedula() : "",
                "foto", p.getUrlFoto() != null ? p.getUrlFoto() : "",
                "prerrequisitos", p.getUrlPrerrequisitos() != null ? p.getUrlPrerrequisitos() : "",
                "nombreCompleto", p.getNombres() + " " + p.getApellidos(),
                "identificacion", p.getIdentificacion()
        ));
    }

    @PutMapping("/prepostulaciones/{id}/estado")
    public ResponseEntity<Map<String, String>> actualizarEstado(
            @PathVariable Long id,
            @RequestBody ActualizarEstadoDTO dto
    ) {
        prepostulacionService.actualizarEstado(
                id,
                dto.getEstado(),
                dto.getObservaciones(),
                dto.getIdRevisor()
        );

        return ResponseEntity.ok(Map.of(
                "mensaje", "Estado actualizado correctamente",
                "nuevoEstado", dto.getEstado()
        ));
    }

    @GetMapping("/prepostulaciones/estadisticas")
    public ResponseEntity<Map<String, Long>> obtenerEstadisticas() {
        List<Prepostulacion> todas = prepostulacionService.listarTodas();

        long total = todas.size();
        long pendientes = todas.stream()
                .filter(p -> "PENDIENTE".equals(p.getEstadoRevision()))
                .count();
        long aprobadas = todas.stream()
                .filter(p -> "APROBADO".equals(p.getEstadoRevision()))
                .count();
        long rechazadas = todas.stream()
                .filter(p -> "RECHAZADO".equals(p.getEstadoRevision()))
                .count();

        return ResponseEntity.ok(Map.of(
                "total", total,
                "pendientes", pendientes,
                "aprobadas", aprobadas,
                "rechazadas", rechazadas
        ));
    }

    @GetMapping("/prepostulaciones/buscar")
    public ResponseEntity<List<Prepostulacion>> buscar(@RequestParam String query) {
        List<Prepostulacion> resultados = prepostulacionService.buscar(query);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/prepostulaciones/resumen")
    public ResponseEntity<List<PrepostulacionDetalleDTO>> listarResumen() {
        List<Prepostulacion> todas = prepostulacionService.listarTodas();

        List<PrepostulacionDetalleDTO> resumen = todas.stream()
                .map(p -> new PrepostulacionDetalleDTO(
                        p.getIdPrepostulacion(),
                        p.getIdentificacion(),
                        p.getNombres() + " " + p.getApellidos(),
                        p.getCorreo(),
                        p.getEstadoRevision(),
                        p.getFechaEnvio(),
                        p.getUrlCedula() != null && p.getUrlFoto() != null && p.getUrlPrerrequisitos() != null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resumen);
    }
}