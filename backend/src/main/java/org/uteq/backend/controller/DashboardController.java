package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudAccion;
import org.uteq.backend.repository.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioRepository usuarioRepository;
    private final PrepostulacionRepository prepostulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final AudAccionRepository audAccionRepository;

    /**
     * Stats resumidas para el dashboard de admin.
     */
    @GetMapping("/admin-stats")
    public ResponseEntity<Map<String, Object>> adminStats() {
        Map<String, Object> stats = new HashMap<>();

        // Usuarios activos = todos los usuarios registrados
        long usuariosActivos = usuarioRepository.count();
        stats.put("usuariosActivos", usuariosActivos);

        // Postulantes pendientes = prepostulaciones con estado PENDIENTE
        long postulantesPendientes = prepostulacionRepository
                .findByEstadoRevision("PENDIENTE").size();
        stats.put("postulantesPendientes", postulantesPendientes);

        // Convocatorias abiertas
        long convocatoriasAbiertas = convocatoriaRepository
                .findByEstadoConvocatoriaOrderByFechaPublicacionDesc("ABIERTA").size();
        long totalConvocatorias = convocatoriaRepository.count();
        stats.put("convocatoriasAbiertas", convocatoriasAbiertas);
        stats.put("totalConvocatorias", totalConvocatorias);

        // Actividad reciente: últimas 5 acciones de auditoría
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> actividad = audAccionRepository
                .findAll(PageRequest.of(0, 5, Sort.by("fecha").descending()))
                .getContent()
                .stream()
                .map(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("descripcion", a.getDescripcion() != null ? a.getDescripcion() : a.getAccion() + " en " + a.getEntidad());
                    item.put("accion", a.getAccion());
                    item.put("entidad", a.getEntidad());
                    item.put("usuario", a.getUsuarioApp());
                    item.put("fecha", a.getFecha() != null ? a.getFecha().format(fmt) : "");
                    return item;
                })
                .collect(Collectors.toList());

        stats.put("actividadReciente", actividad);

        return ResponseEntity.ok(stats);
    }
}
