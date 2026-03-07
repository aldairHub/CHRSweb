package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.NotificacionesResumenDTO;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.JwtService;
import org.uteq.backend.service.NotificacionService;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notifService;
    private final JwtService          jwtService;
    private final UsuarioRepository   usuarioRepo;

    /**
     * GET /api/notificaciones
     * Devuelve las últimas 30 notificaciones del usuario logueado + conteo no leídas.
     * El navbar llama a este endpoint cada 30 segundos.
     */
    @GetMapping
    public ResponseEntity<NotificacionesResumenDTO> obtenerMisNotificaciones(HttpServletRequest request) {
        Long idUsuario = extraerIdUsuario(request);
        return ResponseEntity.ok(notifService.obtenerResumen(idUsuario));
    }

    /**
     * PATCH /api/notificaciones/{id}/leer
     * Marca una notificación específica como leída.
     */
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Map<String, Object>> marcarLeida(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long idUsuario = extraerIdUsuario(request);
        boolean ok = notifService.marcarLeida(id, idUsuario);

        return ResponseEntity.ok(Map.of(
            "ok", ok,
            "mensaje", ok ? "Notificación marcada como leída" : "No se pudo marcar"
        ));
    }

    /**
     * PATCH /api/notificaciones/leer-todas
     * Marca todas las notificaciones no leídas del usuario como leídas.
     */
    @PatchMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(HttpServletRequest request) {
        Long idUsuario = extraerIdUsuario(request);
        int total = notifService.marcarTodasLeidas(idUsuario);

        return ResponseEntity.ok(Map.of(
            "ok", true,
            "total", total,
            "mensaje", total + " notificación(es) marcada(s) como leídas"
        ));
    }

    // ── Helper: extrae id_usuario del JWT ───────────────────────────────────

    private Long extraerIdUsuario(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new RuntimeException("Token no proporcionado");

        String usuarioApp = jwtService.extractUsername(header.substring(7));
        return usuarioRepo.findByUsuarioApp(usuarioApp)
            .map(u -> u.getIdUsuario())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioApp));
    }
}
