package org.uteq.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * POST /api/revisor/reportes/analisis-ia
 *
 * Recibe un resumen de estadísticas (convocatorias, prepostulaciones,
 * solicitudes) y devuelve un análisis ejecutivo generado con Groq/LLaMA.
 * Si el modelo no está disponible, responde con un texto generado localmente.
 */
@Slf4j
@RestController
@RequestMapping("/api/revisor/reportes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReportesIaController {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODELO   = "llama-3.1-8b-instant";

    @Value("${groq.api.token:}")
    private String groqApiToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── DTO interno ───────────────────────────────────────────────────────

    public static class StatsPayload {
        public ConvStats     convocatorias;
        public PrepostStats  prepostulaciones;
        public SolicStats    solicitudes;

        public static class ConvStats {
            public int total, abiertas, cerradas, canceladas;
        }
        public static class PrepostStats {
            public int total, aprobadas, rechazadas, pendientes, tasaAprobacion;
        }
        public static class SolicStats {
            public int total, pendientes, aprobadas, rechazadas, docentesRequeridos;
        }
    }

    // ── Endpoint ──────────────────────────────────────────────────────────

    @PostMapping("/analisis-ia")
    public ResponseEntity<Map<String, String>> analizar(
            @RequestBody StatsPayload stats
    ) {
        String analisis = generarConIA(stats);
        return ResponseEntity.ok(Map.of("analisis", analisis));
    }

    // ── Lógica IA ─────────────────────────────────────────────────────────

    private String generarConIA(StatsPayload s) {
        if (groqApiToken == null || groqApiToken.isBlank()) {
            log.warn("[IA-Reportes] groq.api.token no configurado, usando fallback local.");
            return generarFallback(s);
        }
        try {
            String prompt = buildPrompt(s);
            String body   = buildBody(prompt);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + groqApiToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                log.warn("[IA-Reportes] Groq devolvió {}: {}", resp.statusCode(), resp.body());
                return generarFallback(s);
            }

            JsonNode json     = objectMapper.readTree(resp.body());
            String contenido  = json.path("choices").path(0)
                    .path("message").path("content").asText("").trim();

            log.info("[IA-Reportes] Análisis generado correctamente ({} chars)", contenido.length());
            return contenido.isBlank() ? generarFallback(s) : contenido;

        } catch (Exception e) {
            log.error("[IA-Reportes] Error al llamar Groq: {}", e.getMessage());
            return generarFallback(s);
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────

    private String buildPrompt(StatsPayload s) {
        var c  = s.convocatorias    != null ? s.convocatorias    : new StatsPayload.ConvStats();
        var p  = s.prepostulaciones != null ? s.prepostulaciones : new StatsPayload.PrepostStats();
        var so = s.solicitudes      != null ? s.solicitudes      : new StatsPayload.SolicStats();

        return """
            Eres un analista académico de una universidad ecuatoriana.
            Analiza las siguientes estadísticas del sistema de selección docente
            y redacta un ANÁLISIS EJECUTIVO en español formal, de 4 a 6 oraciones.

            DATOS ACTUALES:
            Convocatorias: %d total | %d abiertas | %d cerradas | %d canceladas
            Prepostulaciones: %d total | %d aprobadas | %d rechazadas | %d pendientes | Tasa aprobación: %d%%
            Solicitudes de docente: %d total | %d aprobadas | %d pendientes | %d rechazadas | Docentes requeridos: %d

            INSTRUCCIONES:
            - Redacta en español formal y académico
            - Entre 4 y 6 oraciones, máximo 120 palabras
            - Incluye observaciones sobre la tasa de aprobación y el estado de las convocatorias
            - Si hay alertas (tasa baja, muchas pendientes, sin convocatorias abiertas), menciónalas
            - Termina con una recomendación concreta
            - Sin listas, encabezados ni formato especial; solo párrafo continuo
            """.formatted(
                c.total, c.abiertas, c.cerradas, c.canceladas,
                p.total, p.aprobadas, p.rechazadas, p.pendientes, p.tasaAprobacion,
                so.total, so.aprobadas, so.pendientes, so.rechazadas, so.docentesRequeridos
        );
    }

    private String buildBody(String prompt) throws Exception {
        var body     = objectMapper.createObjectNode();
        var messages = objectMapper.createArrayNode();
        var msg      = objectMapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        body.put("model", MODELO);
        body.put("max_tokens", 200);
        body.set("messages", messages);
        return objectMapper.writeValueAsString(body);
    }

    // ── Fallback local ────────────────────────────────────────────────────

    private String generarFallback(StatsPayload s) {
        if (s == null) return "No hay datos suficientes para generar un análisis.";

        var c  = s.convocatorias    != null ? s.convocatorias    : new StatsPayload.ConvStats();
        var p  = s.prepostulaciones != null ? s.prepostulaciones : new StatsPayload.PrepostStats();
        var so = s.solicitudes      != null ? s.solicitudes      : new StatsPayload.SolicStats();

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                "El sistema registra %d convocatoria%s, de las cuales %d se encuentran actualmente abiertas. ",
                c.total, c.total != 1 ? "s" : "", c.abiertas
        ));

        if (p.total > 0) {
            sb.append(String.format(
                    "Se han recibido %d prepostulación%s con una tasa de aprobación del %d%%, "
                            + "manteniéndose %d caso%s pendiente%s de revisión. ",
                    p.total, p.total != 1 ? "es" : "",
                    p.tasaAprobacion,
                    p.pendientes, p.pendientes != 1 ? "s" : "", p.pendientes != 1 ? "s" : ""
            ));
        }

        if (so.total > 0) {
            sb.append(String.format(
                    "Se han generado %d solicitud%s de docente con una demanda total de %d docente%s, "
                            + "de las cuales %d han sido aprobadas y %d se encuentran en revisión. ",
                    so.total, so.total != 1 ? "es" : "",
                    so.docentesRequeridos, so.docentesRequeridos != 1 ? "s" : "",
                    so.aprobadas, so.pendientes
            ));
        }

        // Alertas
        if (p.tasaAprobacion < 50 && p.total > 5) {
            sb.append("La tasa de aprobación inferior al 50% sugiere revisar los criterios de admisión o fortalecer la orientación previa a los postulantes. ");
        }
        if (c.abiertas == 0 && so.pendientes > 0) {
            sb.append("Se recomienda abrir nuevas convocatorias para cubrir las solicitudes de docente pendientes de atención. ");
        }
        if (p.pendientes > p.aprobadas && p.pendientes > 3) {
            sb.append("El volumen de prepostulaciones pendientes requiere atención prioritaria para no retrasar el proceso de selección.");
        }

        return sb.toString().trim();
    }
}