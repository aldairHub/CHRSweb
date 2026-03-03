package org.uteq.backend.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Service
public class HuggingFaceImagenService {

    @Value("${huggingface.api.token}") private String apiToken;
    @Value("${huggingface.api.url}")   private String apiUrl;

    public byte[] generarImagen(String prompt) throws IOException, InterruptedException {
        String body = "{\"inputs\": \"" + prompt.replace("\"","\\\"").replace("\n","\\n") + "\"}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (res.statusCode() == 200) return res.body();
        if (res.statusCode() == 503) throw new RuntimeException(
                "El modelo de IA esta iniciando (cold start). Espera 30s e intenta de nuevo.");
        throw new RuntimeException("Error Hugging Face [" + res.statusCode() + "]: " + new String(res.body()));
    }
}