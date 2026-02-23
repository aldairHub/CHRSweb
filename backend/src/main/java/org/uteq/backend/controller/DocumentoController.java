package org.uteq.backend.controller;


import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// ============================================================
// DocumentoController
// Base URL: /api/documentos
// ============================================================
@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "http://localhost:4200")  // Ajusta al puerto de tu Angular
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    // ----------------------------------------------------------
    // GET /api/documentos/postulacion/{idPostulacion}
    // Obtiene todos los tipos de doc + estado del postulante
    // Llama: SP sp_obtener_documentos_postulacion
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
    // POST /api/documentos/subir
    // Sube un archivo PDF y lo registra en BD
    // Llama: SP sp_guardar_documento
    // ----------------------------------------------------------
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> subirDocumento(
            @RequestParam("idPostulacion")    Long idPostulacion,
            @RequestParam("idTipoDocumento")  Long idTipoDocumento,
            @RequestPart("archivo")           MultipartFile archivo   // ← cambiar @RequestParam por @RequestPart
    ) {
        try {
            Map<String, Object> resultado = documentoService.subirDocumento(
                    idPostulacion, idTipoDocumento, archivo
            );
            boolean exitoso = Boolean.TRUE.equals(resultado.get("exitoso"));
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exitoso", false, "mensaje", "Error al guardar el archivo: " + e.getMessage()));
        }
    }

    // ----------------------------------------------------------
    // DELETE /api/documentos/{idDocumento}/postulacion/{idPostulacion}
    // Elimina un documento (solo si está en estado 'pendiente')
    // Llama: SP sp_eliminar_documento
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
    // POST /api/documentos/finalizar/{idPostulacion}
    // Finaliza la carga → cambia estado postulación a 'en_revision'
    // Llama: SP sp_finalizar_carga_documentos
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
    // GET /api/documentos/postulante/{idUsuario}
    // Obtiene info del postulante y su postulación activa
    // Llama: SP sp_obtener_info_postulante
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
}