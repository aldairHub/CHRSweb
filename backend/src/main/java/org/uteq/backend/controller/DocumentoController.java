package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.uteq.backend.dto.DocPrepostulacionDTO;
import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.repository.UsuarioRepository;
import org.uteq.backend.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.service.JwtService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "http://localhost:4200")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepo;


    // ----------------------------------------------------------
    // Obtiene todos los tipos de doc + estado del postulante
    // ----------------------------------------------------------
    @GetMapping("/postulacion/{idPostulacion}")
    public ResponseEntity<List<DocumentoResponseDTO>> obtenerDocumentos(
            @PathVariable Long idPostulacion
    ) {
        List<DocumentoResponseDTO> documentos =
                documentoService.obtenerDocumentosPostulacion(idPostulacion);
        return ResponseEntity.ok(documentos);
    }

    // ----------------------------------------------------------
    // Sube un archivo PDF a Supabase y lo registra en BD
    // ----------------------------------------------------------
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> subirDocumento(
            @RequestParam("idPostulacion")   Long idPostulacion,
            @RequestParam("idTipoDocumento") Long idTipoDocumento,
            @RequestPart("archivo")          MultipartFile archivo
    ) {
        try {
            Map<String, Object> resultado = documentoService.subirDocumento(
                    idPostulacion, idTipoDocumento, archivo
            );
            boolean exitoso = Boolean.TRUE.equals(resultado.get("exitoso"));
            return exitoso
                    ? ResponseEntity.ok(resultado)
                    : ResponseEntity.badRequest().body(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exitoso", false, "mensaje", "Error inesperado: " + e.getMessage()));
        }
    }

    // ----------------------------------------------------------
    // Elimina un documento (solo si está en estado 'pendiente')
    // ----------------------------------------------------------
    @DeleteMapping("/{idDocumento}/postulacion/{idPostulacion}")
    public ResponseEntity<Map<String, Object>> eliminarDocumento(
            @PathVariable Long idDocumento,
            @PathVariable Long idPostulacion
    ) {
        Map<String, Object> resultado = documentoService.eliminarDocumento(idDocumento, idPostulacion);
        boolean eliminado = Boolean.TRUE.equals(resultado.get("eliminado"));
        return eliminado
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.badRequest().body(resultado);
    }

    // ----------------------------------------------------------
    // Finaliza la carga → cambia estado postulación a 'en_revision'
    // ----------------------------------------------------------
    @PostMapping("/finalizar/{idPostulacion}")
    public ResponseEntity<Map<String, Object>> finalizarCarga(
            @PathVariable Long idPostulacion
    ) {
        Map<String, Object> resultado = documentoService.finalizarCarga(idPostulacion);
        boolean exitoso = Boolean.TRUE.equals(resultado.get("exitoso"));
        return exitoso
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.badRequest().body(resultado);
    }

    // ----------------------------------------------------------
    // Obtiene info del postulante y su postulación activa
    // ----------------------------------------------------------
    @GetMapping("/postulante/{idUsuario}")
    public ResponseEntity<PostulanteInfoDTO> obtenerInfoPostulante(
            @PathVariable Long idUsuario
    ) {
        PostulanteInfoDTO info = documentoService.obtenerInfoPostulante(idUsuario);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @GetMapping("/convocatoria/{idPostulacion}")
    public ResponseEntity<List<DocumentoResponseDTO>> obtenerDocumentosConvocatoria(
            @PathVariable Long idPostulacion) {
        return ResponseEntity.ok(documentoService.obtenerDocumentosConvocatoria(idPostulacion));
    }

    // ----------------------------------------------------------
    // Valida o rechaza un documento individual (uso del evaluador)
    // ----------------------------------------------------------
    @PostMapping("/validar/{idDocumento}")
    public ResponseEntity<Map<String, Object>> validarDocumento(
            @PathVariable Long idDocumento,
            @RequestBody Map<String, String> body
    ) {
        String estado      = body.getOrDefault("estado", "pendiente");
        String observacion = body.getOrDefault("observacion", "");
        Map<String, Object> resultado = documentoService.validarDocumento(idDocumento, estado, observacion);
        return ResponseEntity.ok(resultado);
    }

    // ----------------------------------------------------------
    // Info del postulante a partir del ID de postulación (uso evaluador)
    // ----------------------------------------------------------
    @GetMapping("/info-postulacion/{idPostulacion}")
    public ResponseEntity<Map<String, Object>> infoPostulacion(
            @PathVariable Long idPostulacion
    ) {
        Map<String, Object> info = documentoService.obtenerInfoPorPostulacion(idPostulacion);
        if (info == null || info.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/prepostulacion/{idPostulacion}")
    public ResponseEntity<List<DocPrepostulacionDTO>> obtenerDocsPrepostulacion(
            @PathVariable Long idPostulacion) {
        return ResponseEntity.ok(documentoService.obtenerDocsPrepostulacion(idPostulacion));
    }

    @PostMapping("/notificar-revision/{idPostulacion}")
    public ResponseEntity<Map<String, Object>> notificarRevision(
            @PathVariable Long idPostulacion,
            HttpServletRequest request) {
        Long idUsuario = extraerIdUsuario(request);
        return ResponseEntity.ok(documentoService.enviarARevision(idPostulacion, idUsuario));
    }

    private Long extraerIdUsuario(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new RuntimeException("Token no proporcionado");
        String usuarioApp = jwtService.extractUsername(header.substring(7));
        return usuarioRepo.findByUsuarioApp(usuarioApp)
                .map(u -> u.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @GetMapping("/resultados/{idUsuario}")
    public ResponseEntity<Map<String, Object>> resultadosPostulante(
            @PathVariable Long idUsuario) {
        Map<String, Object> resultado = documentoService.obtenerResultadosPostulante(idUsuario);
        if (resultado.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(resultado);
    }

    // ── ENDPOINT 1: Listar todas las postulaciones del postulante ──────────────
// Coloca esto DENTRO de la clase DocumentoController, después del endpoint existente
// "/postulante/{idUsuario}"

    /**
     * GET /api/documentos/postulante/{idUsuario}/postulaciones
     * Devuelve todas las postulaciones activas del usuario para el filtro de convocatoria.
     */
    @GetMapping("/postulante/{idUsuario}/postulaciones")
    public ResponseEntity<List<PostulanteInfoDTO>> listarPostulacionesUsuario(
            @PathVariable Long idUsuario
    ) {
        List<PostulanteInfoDTO> postulaciones = documentoService.listarPostulacionesUsuario(idUsuario);
        return ResponseEntity.ok(postulaciones);
    }

// ── ENDPOINT 2: Info de postulante filtrada por idPostulacion específica ────
// Coloca esto después del endpoint anterior

    /**
     * GET /api/documentos/postulante/{idUsuario}/postulacion/{idPostulacion}
     * Devuelve la info del postulante para UNA postulación específica (filtro activo).
     */
    @GetMapping("/postulante/{idUsuario}/postulacion/{idPostulacion}")
    public ResponseEntity<PostulanteInfoDTO> obtenerInfoPostulanteConvocatoria(
            @PathVariable Long idUsuario,
            @PathVariable Long idPostulacion
    ) {
        PostulanteInfoDTO info = documentoService.obtenerInfoPostulantePorPostulacion(idUsuario, idPostulacion);
        if (info == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(info);
    }

// ── ENDPOINT 3: Progreso en tiempo real del proceso del postulante ──────────
// GET /api/documentos/postulante/{idUsuario}/progreso?idPostulacion=X

    /**
     * GET /api/documentos/postulante/{idUsuario}/progreso
     * Devuelve el ProcesoEvaluacion del postulante para la postulación indicada:
     *   - faseActual, progreso (0-100), estadoGeneral, fases con sus estados
     * El frontend hace polling cada 15 s para "tiempo real".
     */
    @GetMapping("/postulante/{idUsuario}/progreso")
    public ResponseEntity<Map<String, Object>> obtenerProgreso(
            @PathVariable Long idUsuario,
            @RequestParam(required = false) Long idPostulacion
    ) {
        Map<String, Object> progreso = documentoService.obtenerProgresoPostulante(idUsuario, idPostulacion);
        if (progreso == null) return ResponseEntity.ok(Map.of("sinProceso", true));
        return ResponseEntity.ok(progreso);
    }

}