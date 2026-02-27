package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.EvaluacionRequestDTO;
import org.uteq.backend.dto.EvaluacionResponseDTO;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.EvaluacionService;
import org.uteq.backend.service.JwtService;

import java.util.List;

/**
 * Registro de Evaluaciones por parte de los evaluadores.
 * Endpoints consumidos por el componente evaluacion del frontend.
 */
@RestController
@RequestMapping("/api/evaluacion/evaluaciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService service;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Confirmar y enviar la evaluación de un evaluador para una reunión.
     * El idEvaluador se extrae del JWT para garantizar que cada evaluador
     * solo pueda enviar en su propio nombre.
     */
    @PostMapping
    public ResponseEntity<EvaluacionResponseDTO> confirmar(
            @RequestBody EvaluacionRequestDTO dto,
            HttpServletRequest request) {

        Long idEvaluador = extraerIdUsuario(request);
        return ResponseEntity.status(201).body(service.confirmar(dto, idEvaluador));
    }

    /** Listar evaluaciones de una reunión (para el panel de resultados) */
    @GetMapping("/reunion/{idReunion}")
    public ResponseEntity<List<EvaluacionResponseDTO>> porReunion(@PathVariable Long idReunion) {
        return ResponseEntity.ok(service.obtenerPorReunion(idReunion));
    }

    // ─── Helper ────────────────────────────────────────────────

    private Long extraerIdUsuario(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new RuntimeException("Token no proporcionado");

        String usuarioApp = jwtService.extractUsername(header.substring(7));
        return usuarioRepository.findByUsuarioApp(usuarioApp)
                .map(u -> u.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioApp));
    }
}
