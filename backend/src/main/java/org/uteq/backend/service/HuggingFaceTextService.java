package org.uteq.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HuggingFaceTextService implements AiTextService {

    // 1. Renombrar la constante URL (opcional pero más claro)
    private static final String HF_TEXT_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // 2. Cambiar el modelo
    private static final String MODELO = "llama-3.1-8b-instant";

    @Value("${groq.api.token}")          // nueva
    private String groqApiToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateConvocatoriaDescripcion(List<SolicitudContexto> contextos) {
        if (contextos == null || contextos.isEmpty()) {
            return fallback(null);
        }

        try {
            String prompt       = buildPrompt(contextos);
            String requestBody  = buildRequestBody(prompt);

            log.info("[IA] Llamando a HuggingFace Text API con {} solicitudes", contextos.size());

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HF_TEXT_URL))
                    .header("Authorization", "Bearer " + groqApiToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.info("[IA] HuggingFace respondió con status {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.warn("[IA] Error de HuggingFace [{}]: {}", response.statusCode(), response.body());
                return fallback(contextos);
            }

            JsonNode json    = objectMapper.readTree(response.body());
            String contenido = json.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("").trim();

            log.info("[IA] Descripción generada exitosamente ({} chars)", contenido.length());
            return contenido.isBlank() ? fallback(contextos) : contenido;

        } catch (Exception e) {
            log.error("[IA] Error al contactar HuggingFace: {}", e.getMessage(), e);
            return fallback(contextos);
        }
    }

    // ─── Prompt enriquecido con nombre de materia ─────────────────────────

    private String buildPrompt(List<SolicitudContexto> contextos) {
        String textos = contextos.stream()
                .filter(c -> c.justificacion() != null && !c.justificacion().isBlank())
                .map(c -> {
                    if (c.nombreMateria() != null && !c.nombreMateria().isBlank()) {
                        return "Materia: " + c.nombreMateria() + "\nJustificación: " + c.justificacion();
                    }
                    return "Justificación: " + c.justificacion();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String materias = contextos.stream()
                .map(SolicitudContexto::nombreMateria)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return """
            Eres un asistente académico especializado en redacción institucional universitaria.

            Tu tarea es generar una descripción formal y profesional para una CONVOCATORIA DOCENTE
            de la Universidad Técnica Estatal de Quevedo (UTEQ), basándote en las siguientes
            solicitudes de contratación:

            %s

            %s

            INSTRUCCIONES:
            - Escribe en español formal y académico
            - Máximo 2 oraciones cortas, no más de 60 palabras en total
            - Menciona las materias o áreas involucradas
            - Sin encabezados, listas ni formato especial; solo texto plano
            - Sin saludos, despedidas ni comentarios adicionales

            Genera ÚNICAMENTE la descripción, sin texto previo ni posterior.
            """.formatted(
                textos,
                materias.isBlank() ? "" : "Materias involucradas: " + materias
        );
    }

    // ─── Request body ─────────────────────────────────────────────────────

    private String buildRequestBody(String prompt) throws Exception {
        var body = objectMapper.createObjectNode();
        body.put("model", MODELO);
        body.put("max_tokens", 120);

        var messages = objectMapper.createArrayNode();
        var msg = objectMapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);

        body.set("messages", messages);
        return objectMapper.writeValueAsString(body);
    }

    // ─── Fallback dinámico según las materias disponibles ─────────────────

    private String fallback(List<SolicitudContexto> contextos) {
        if (contextos == null || contextos.isEmpty()) {
            return fallbackGenerico();
        }

        String materias = contextos.stream()
                .map(SolicitudContexto::nombreMateria)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        if (materias.isBlank()) {
            return fallbackGenerico();
        }

        return "La presente convocatoria tiene como objetivo seleccionar docentes calificados "
                + "para cubrir las necesidades académicas en las áreas de " + materias + ", "
                + "en concordancia con el plan curricular vigente y los requerimientos de formación "
                + "profesional de los estudiantes de la institución. Se invita a profesionales con el "
                + "perfil requerido a participar en el proceso de selección conforme a los criterios "
                + "y requisitos establecidos por la universidad.";
    }

    private String fallbackGenerico() {
        return "La presente convocatoria tiene como objetivo cubrir las necesidades académicas "
                + "identificadas por las autoridades de la institución, en concordancia con el plan "
                + "curricular vigente y los requerimientos de formación profesional de los estudiantes. "
                + "Se invita a profesionales calificados a participar en el proceso de selección de "
                + "acuerdo con los requisitos establecidos.";
    }
}