package org.uteq.backend.service;

import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.repository.DocumentoRepositoryCustomImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
