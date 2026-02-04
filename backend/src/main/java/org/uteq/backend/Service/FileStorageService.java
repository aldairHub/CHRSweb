package org.uteq.backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Guarda un archivo en el sistema de archivos
     * @param file Archivo a guardar
     * @param subfolder Subcarpeta dentro de uploads (ej: "cedulas", "fotos", "prerrequisitos")
     * @return Ruta relativa del archivo guardado
     */
    public String guardarArchivo(MultipartFile file, String subfolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        // Crear directorio si no existe
        Path uploadPath = Paths.get(uploadDir, subfolder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generar nombre único para evitar colisiones
        String nombreOriginal = file.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }
        String nombreUnico = UUID.randomUUID().toString() + extension;

        // Guardar archivo
        Path archivoPath = uploadPath.resolve(nombreUnico);
        Files.copy(file.getInputStream(), archivoPath, StandardCopyOption.REPLACE_EXISTING);

        // Retornar ruta relativa
        return subfolder + "/" + nombreUnico;
    }

    /**
     * Elimina un archivo del sistema
     * @param rutaRelativa Ruta relativa del archivo
     */
    public void eliminarArchivo(String rutaRelativa) throws IOException {
        if (rutaRelativa == null || rutaRelativa.isEmpty()) {
            return;
        }
        Path archivoPath = Paths.get(uploadDir, rutaRelativa);
        if (Files.exists(archivoPath)) {
            Files.delete(archivoPath);
        }
    }

    /**
     * Obtiene la ruta absoluta de un archivo
     * @param rutaRelativa Ruta relativa del archivo
     * @return Path absoluto
     */
    public Path obtenerRutaAbsoluta(String rutaRelativa) {
        return Paths.get(uploadDir, rutaRelativa);
    }
}