package org.uteq.backend.service;

import org.uteq.backend.dto.DocumentoResponseDTO;
import org.uteq.backend.dto.PostulanteInfoDTO;
import org.uteq.backend.repository.DocumentoRepositoryCustomImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ============================================================
// DocumentoService
// Lógica de negocio: almacenamiento de archivos + llamadas a SPs
// ============================================================
@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepositoryCustomImpl documentoRepo;

    // Configura la ruta base en application.properties:
    // app.upload.dir=/uploads/documentos
    @Value("${app.upload.dir:/uploads/documentos}")
    private String uploadDir;

    // ----------------------------------------------------------
    // Obtener documentos de una postulación (SP 1)
    // ----------------------------------------------------------
    public List<DocumentoResponseDTO> obtenerDocumentosPostulacion(Long idPostulacion) {
        return documentoRepo.obtenerDocumentosPostulacion(idPostulacion);
    }

    // ----------------------------------------------------------
    // Subir archivo y registrar en BD (SP 2)
    // ----------------------------------------------------------
    public Map<String, Object> subirDocumento(
            Long idPostulacion,
            Long idTipoDocumento,
            MultipartFile archivo
    ) throws IOException {

        Map<String, Object> response = new HashMap<>();

        // Validar tipo
        if (archivo.isEmpty()) {
            response.put("exitoso", false);
            response.put("mensaje", "El archivo está vacío.");
            return response;
        }

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

        // Guardar físicamente el archivo
        String nombreUnico = UUID.randomUUID() + "_" + archivo.getOriginalFilename();
        String subCarpeta  = "postulacion_" + idPostulacion;
        Path ruta = Paths.get(uploadDir, subCarpeta, nombreUnico);
        Files.createDirectories(ruta.getParent());
        archivo.transferTo(ruta.toFile());

        // Ruta relativa que se guarda en BD
        String rutaBD = subCarpeta + "/" + nombreUnico;

        // Llamar SP 2
        Map<String, Object> resultado = documentoRepo.guardarDocumento(
                idPostulacion, idTipoDocumento, rutaBD
        );

        String mensaje = (String) resultado.get("mensaje");

        if (mensaje != null && mensaje.startsWith("ERROR")) {
            // Si el SP falló, eliminar el archivo ya subido
            Files.deleteIfExists(ruta);
            response.put("exitoso", false);
            response.put("mensaje", mensaje);
        } else {
            response.put("exitoso", true);
            response.put("mensaje", "Documento subido correctamente.");
            response.put("idDocumento", resultado.get("idDocumento"));
            response.put("rutaArchivo", rutaBD);
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