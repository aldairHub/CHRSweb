package org.uteq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    /**
     * Sube un archivo a Supabase Storage
     * @param archivo - El archivo a subir (MultipartFile)
     * @param carpeta - Carpeta dentro del bucket (ej: "cedulas", "fotos", "prerrequisitos")
     * @param identificador - Identificación del usuario (para nombrar el archivo)
     * @return URL pública del archivo subido
     */
    public String subirArchivo(MultipartFile archivo, String carpeta, String identificador) throws IOException, InterruptedException {

        // Validar que el archivo no esté vacío
        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        // Obtener extensión del archivo
        String nombreOriginal = archivo.getOriginalFilename();
        String extension = nombreOriginal != null && nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf("."))
                : "";

        // Generar nombre único: carpeta/identificador_uuid.extension
        String nombreArchivo = carpeta + "/" + identificador + "_" + UUID.randomUUID().toString() + extension;

        // URL del endpoint de Supabase Storage
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + nombreArchivo;

        System.out.println("📤 Subiendo archivo a: " + url);

        // Crear cliente HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crear petición POST con el archivo
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", archivo.getContentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(archivo.getBytes()))
                .build();

        // Enviar petición
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Verificar respuesta
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            // URL pública del archivo
            String urlPublica = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + nombreArchivo;
            System.out.println("✅ Archivo subido exitosamente: " + urlPublica);
            return urlPublica;
        } else {
            System.err.println("❌ Error al subir archivo. Status: " + response.statusCode());
            System.err.println("Respuesta: " + response.body());
            throw new RuntimeException("Error al subir archivo a Supabase: " + response.body());
        }
    }

    /**
     * Elimina un archivo de Supabase Storage
     * @param nombreArchivo - Ruta completa del archivo en el bucket
     */
    public void eliminarArchivo(String nombreArchivo) throws IOException, InterruptedException {
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + nombreArchivo;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + supabaseKey)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error al eliminar archivo: " + response.body());
        }

        System.out.println("🗑️ Archivo eliminado: " + nombreArchivo);
    }



    public String subirBytes(byte[] bytes, String carpeta, String nombreBase,
                             String extension, String bucketDestino)
            throws IOException, InterruptedException {

        String nombre = carpeta + "/" + nombreBase + "_" + UUID.randomUUID() + extension;
        String url = supabaseUrl + "/storage/v1/object/" + bucketDestino + "/" + nombre;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", "image/jpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200 || res.statusCode() == 201)
            return supabaseUrl + "/storage/v1/object/public/" + bucketDestino + "/" + nombre;
        throw new RuntimeException("Error subiendo imagen: " + res.body());
    }
}