package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.Prepostulacion;
import org.uteq.backend.service.PrepostulacionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/prepostulaciones")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RequiredArgsConstructor
public class AdminPrepostulacionController {

    private final PrepostulacionService prepostulacionService;

    /**
     * Listar todas las prepostulaciones con URL de documentos
     * GET http://localhost:8080/api/admin/prepostulaciones
     */
    @GetMapping
    public ResponseEntity<List<Prepostulacion>> listarPrepostulaciones() {
        List<Prepostulacion> prepostulaciones = prepostulacionService.listarTodas();
        return ResponseEntity.ok(prepostulaciones);
    }

    /**
     * Listar prepostulaciones por estado
     * GET http://localhost:8080/api/admin/prepostulaciones/estado/PENDIENTE
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Prepostulacion>> listarPorEstado(@PathVariable String estado) {
        List<Prepostulacion> prepostulaciones = prepostulacionService.listarPorEstado(estado);
        return ResponseEntity.ok(prepostulaciones);
    }

    /**
     * Obtener URLs de documentos de una prepostulación específica
     * GET http://localhost:8080/api/admin/prepostulaciones/1/documentos
     */
    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> obtenerDocumentos(@PathVariable Long id) {
        Prepostulacion p = prepostulacionService.obtenerPorId(id);

        return ResponseEntity.ok(Map.of(
                "cedula", p.getUrlCedula() != null ? p.getUrlCedula() : "",
                "foto", p.getUrlFoto() != null ? p.getUrlFoto() : "",
                "prerrequisitos", p.getUrlPrerrequisitos() != null ? p.getUrlPrerrequisitos() : "",
                "nombreCompleto", p.getNombres() + " " + p.getApellidos(),
                "identificacion", p.getIdentificacion()
        ));
    }

    /**
     * Obtener una prepostulación específica con todos sus datos
     * GET http://localhost:8080/api/admin/prepostulaciones/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prepostulacion> obtenerPrepostulacion(@PathVariable Long id) {
        Prepostulacion p = prepostulacionService.obtenerPorId(id);
        return ResponseEntity.ok(p);
    }

    /**
     * Actualizar estado de una prepostulación
     * PUT http://localhost:8080/api/admin/prepostulaciones/1/estado
     * Body: { "estado": "APROBADO", "observaciones": "Todo correcto", "idRevisor": 1 }
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        String nuevoEstado = (String) body.get("estado");
        String observaciones = (String) body.get("observaciones");
        Long idRevisor = body.get("idRevisor") != null
                ? ((Number) body.get("idRevisor")).longValue()
                : null;

        prepostulacionService.actualizarEstado(id, nuevoEstado, observaciones, idRevisor);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Estado actualizado correctamente",
                "nuevoEstado", nuevoEstado
        ));
    }
}