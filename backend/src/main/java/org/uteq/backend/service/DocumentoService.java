package org.uteq.backend.service;

import org.uteq.backend.dto.DocPrepostulacionDTO;
import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.repository.DocumentoRepositoryCustomImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ============================================================
// DocumentoService
// Lógica de negocio: almacenamiento en Supabase + llamadas a SPs
// ============================================================
@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepositoryCustomImpl documentoRepo;

    @Autowired
    private SupabaseStorageService supabaseService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private JdbcTemplate jdbc;

    // ----------------------------------------------------------
    // Obtener documentos de una postulación (SP 1)
    // ----------------------------------------------------------
    public List<DocumentoResponseDTO> obtenerDocumentosPostulacion(Long idPostulacion) {
        return documentoRepo.obtenerDocumentosPostulacion(idPostulacion);
    }

    // ----------------------------------------------------------
    // Subir archivo a Supabase y registrar en BD (SP 2)
    // ----------------------------------------------------------
    public Map<String, Object> subirDocumento(
            Long idPostulacion,
            Long idTipoDocumento,
            MultipartFile archivo
    ) {
        Map<String, Object> response = new HashMap<>();

        // Validar que el archivo no esté vacío
        if (archivo == null || archivo.isEmpty()) {
            response.put("exitoso", false);
            response.put("mensaje", "El archivo está vacío.");
            return response;
        }

        // Validar tipo (solo PDF)
        String contentType = archivo.getContentType();
        if (!"application/pdf".equals(contentType)) {
            response.put("exitoso", false);
            response.put("mensaje", "Solo se aceptan archivos PDF.");
            return response;
        }

        // Validar tamaño (10 MB)
        if (archivo.getSize() > 10 * 1024 * 1024) {
            response.put("exitoso", false);
            response.put("mensaje", "El archivo supera el tamaño máximo de 10 MB.");
            return response;
        }

        // ✅ SUBIR ARCHIVO A SUPABASE (carpeta "documentos")
        String urlArchivo;
        try {
            System.out.println("📤 Subiendo documento a Supabase...");
            String identificador = "postulacion_" + idPostulacion + "_tipo_" + idTipoDocumento
                    + "_" + UUID.randomUUID();
            urlArchivo = supabaseService.subirArchivo(
                    archivo,
                    "documentos",
                    identificador
            );
            System.out.println("✅ Documento subido exitosamente: " + urlArchivo);
        } catch (Exception e) {
            System.err.println("❌ Error al subir documento a Supabase: " + e.getMessage());
            e.printStackTrace();
            response.put("exitoso", false);
            response.put("mensaje", "Error al subir el archivo: " + e.getMessage());
            return response;
        }

        // Llamar SP 2 — se guarda la URL pública de Supabase en lugar de una ruta local
        Map<String, Object> resultado = documentoRepo.guardarDocumento(
                idPostulacion, idTipoDocumento, urlArchivo
        );

        String mensaje = (String) resultado.get("mensaje");

        if (mensaje != null && mensaje.startsWith("ERROR")) {
            // Si el SP falló, intentar eliminar el archivo ya subido en Supabase
            try {
                supabaseService.eliminarArchivo(urlArchivo);
            } catch (Exception ex) {
                System.err.println("⚠️ No se pudo eliminar el archivo de Supabase tras fallo del SP: " + ex.getMessage());
            }
            response.put("exitoso", false);
            response.put("mensaje", mensaje);
        } else {
            response.put("exitoso", true);
            response.put("mensaje", "Documento subido correctamente.");
            response.put("idDocumento", resultado.get("idDocumento"));
            response.put("rutaArchivo", urlArchivo);
        }

        return response;
    }

    // ----------------------------------------------------------
    // Eliminar un documento (SP 3)
    // ----------------------------------------------------------
    public Map<String, Object> eliminarDocumento(Long idDocumento, Long idPostulacion) {
        return documentoRepo.eliminarDocumento(idDocumento, idPostulacion);
    }

    // ----------------------------------------------------------
    // Finalizar carga (SP 4)
    // ----------------------------------------------------------
    public Map<String, Object> finalizarCarga(Long idPostulacion) {
        return documentoRepo.finalizarCargaDocumentos(idPostulacion);
    }
    // ----------------------------------------------------------
    // Info del postulante (SP 5) — para el header del componente
    // ----------------------------------------------------------
    public PostulanteInfoDTO obtenerInfoPostulante(Long idUsuario) {
        return documentoRepo.obtenerInfoPostulante(idUsuario);
    }

    public List<DocumentoResponseDTO> obtenerDocumentosConvocatoria(Long idPostulacion) {
        return documentoRepo.obtenerDocumentosConvocatoria(idPostulacion);
    }

    public List<DocPrepostulacionDTO> obtenerDocsPrepostulacion(Long idPostulacion) {
        return documentoRepo.obtenerDocsPrepostulacion(idPostulacion);
    }

    public Map<String, Object> validarDocumento(Long idDocumento, String estado, String observacion) {
        Map<String, Object> result = documentoRepo.validarDocumento(idDocumento, estado, observacion);

        // Notificar al postulante según el resultado de la validación
        if ("rechazado".equalsIgnoreCase(estado) || "validado".equalsIgnoreCase(estado)) {
            try {
                Map<String, Object> docInfo = jdbc.queryForMap(
                        "SELECT d.id_postulacion, p.id_usuario, td.nombre AS tipo_doc " +
                                "FROM documento d " +
                                "JOIN postulacion pc ON pc.id_postulacion = d.id_postulacion " +
                                "JOIN postulante p ON p.id_postulante = pc.id_postulante " +
                                "JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                                "WHERE d.id_documento = ?", idDocumento
                );
                Long idUsuario = ((Number) docInfo.get("id_usuario")).longValue();
                String tipoDoc = (String) docInfo.getOrDefault("tipo_doc", "documento");

                if ("validado".equalsIgnoreCase(estado)) {
                    notificacionService.notificarUsuario(
                            idUsuario, "success",
                            "Documento validado",
                            "Tu documento '" + tipoDoc + "' ha sido validado correctamente. ¡Sigue subiendo los documentos restantes!",
                            "POSTULACION", null
                    );
                } else {
                    String obs = (observacion != null && !observacion.isBlank()) ? observacion : "Sin observación";
                    notificacionService.notificarUsuario(
                            idUsuario, "warning",
                            "Documento rechazado",
                            "Tu documento '" + tipoDoc + "' fue rechazado. Motivo: " + obs + ". Por favor, súbelo nuevamente.",
                            "POSTULACION", null
                    );
                }
            } catch (Exception ignored) {}
        }

        return result;
    }

    public Map<String, Object> obtenerInfoPorPostulacion(Long idPostulacion) {
        return documentoRepo.obtenerInfoPorPostulacion(idPostulacion);
    }

    public Map<String, Object> enviarARevision(Long idPostulacion, Long idUsuarioPostulante) {
        try {
            Map<String, Object> result = jdbc.queryForMap(
                    "SELECT v_exitoso, v_mensaje FROM sp_enviar_a_revision(?)", idPostulacion
            );

            boolean exitoso = Boolean.TRUE.equals(result.get("v_exitoso"));
            if (!exitoso) {
                return Map.of("exitoso", false, "mensaje", result.get("v_mensaje"));
            }

            // Obtener nombre del postulante para la notificación
            String nombrePostulante = "";
            try {
                Map<String, Object> info = jdbc.queryForMap(
                        "SELECT nombres_postulante || ' ' || apellidos_postulante AS nombre " +
                                "FROM postulante WHERE id_usuario = ?", idUsuarioPostulante
                );
                nombrePostulante = (String) info.get("nombre");
            } catch (Exception ignored) {}

            // Notificar a todos los evaluadores
            notificacionService.notificarRol(
                    "evaluador", "info",
                    "Documentos listos para revisión",
                    "El postulante " + nombrePostulante + " ha enviado sus documentos y están listos para revisión.",
                    "POSTULACION", idPostulacion
            );

            return Map.of("exitoso", true, "mensaje", "Documentos enviados a revisión.");

        } catch (Exception e) {
            return Map.of("exitoso", false, "mensaje", "Error al enviar a revisión.");
        }
    }

    public Map<String, Object> obtenerResultadosPostulante(Long idUsuario) {
        return documentoRepo.obtenerResultadosPostulante(idUsuario);
    }

}