package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ReportePrepostulacionConfigDTO;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.ConvocatoriaRepository;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.ReportePrepostulacionService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/prepostulaciones/reporte")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RequiredArgsConstructor
public class ReportePrepostulacionController {

    private final ReportePrepostulacionService reporteService;
    private final ConvocatoriaRepository       convRepo;
    private final UsuarioRepository            usuarioRepo;

    @PostMapping("/generar")
    public ResponseEntity<byte[]> generar(@RequestBody ReportePrepostulacionConfigDTO cfg) {

        // Obtener el usuario autenticado desde el JWT
        String usuarioApp = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepo.findByUsuarioApp(usuarioApp).orElse(null);

        byte[] archivo   = reporteService.generar(cfg, usuario);
        String nombre    = reporteService.nombreArchivo(cfg);
        String mediaType = "EXCEL".equalsIgnoreCase(cfg.getFormato())
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .header(HttpHeaders.CONTENT_TYPE, mediaType)
                .contentLength(archivo.length)
                .body(archivo);
    }

    @GetMapping("/convocatorias")
    public ResponseEntity<List<Map<String, Object>>> listarConvocatorias() {
        List<Map<String, Object>> lista = convRepo.findAllByOrderByFechaPublicacionDesc()
                .stream()
                .map(c -> Map.<String, Object>of(
                        "id",     c.getIdConvocatoria(),
                        "titulo", c.getTitulo(),
                        "estado", c.getEstadoConvocatoria()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }
}