package org.uteq.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.RankingPrepostulacionesRequestDTO;
import org.uteq.backend.dto.RankingResultadoDTO;
import org.uteq.backend.entity.*;
import org.uteq.backend.repository.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que consulta Mistral AI para analizar pre-postulantes
 * y generar un ranking según los criterios seleccionados por el revisor.
 *
 * Criterios soportados:
 *   - documentos   → documentos subidos vs requeridos por la solicitud
 *   - nivel        → nivel académico del pre-postulante vs el requerido
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MistralRankingService {

    // ── Mistral API ───────────────────────────────────────────────────────────
    private static final String MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String MODELO      = "mistral-small-latest";

    @Value("${mistral.api.key}")
    private String mistralApiKey;

    // ── Repositorios ─────────────────────────────────────────────────────────
    private final PrepostulacionRepository              prepostulacionRepository;
    private final PrepostulacionDocumentoRepository     documentoRepository;
    private final PrepostulacionSolicitudRepository     prepostulacionSolicitudRepository;
    private final SolicitudDocenteRepository            solicitudDocenteRepository;
    private final RequisitoPrepostulacionRepository     requisitoRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTODO PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Genera el ranking de pre-postulantes para una solicitud concreta.
     *
     * @param request  criterios elegidos por el revisor desde el modal
     * @return lista ordenada de ResultadoDTO con posición, nombre, score y justificación
     */
    public List<RankingResultadoDTO> generarRanking(RankingPrepostulacionesRequestDTO request) {

        Long idSolicitud = request.getIdSolicitud();

        // 1. Requisitos de la solicitud (documentos obligatorios)
        List<RequisitoPrepostulacion> requisitos =
                requisitoRepository.findByIdSolicitudAndActivoTrueOrderByOrdenAscIdRequisitoAsc(idSolicitud);

        // 2. Datos de la solicitud (nivel académico requerido)
        SolicitudDocente solicitud = solicitudDocenteRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + idSolicitud));

        // 3. Todos los pre-postulantes vinculados a esta solicitud
        List<Long> idsPrepostulacion = prepostulacionSolicitudRepository
                .findByIdIdSolicitud(idSolicitud)
                .stream()
                .map(ps -> ps.getId().getIdPrepostulacion())
                .collect(Collectors.toList());

        if (idsPrepostulacion.isEmpty()) {
            return Collections.emptyList();
        }

        List<Prepostulacion> prepostulantes = prepostulacionRepository.findAllById(idsPrepostulacion);

        // 4. Construir contexto de cada pre-postulante
        List<Map<String, Object>> perfiles = prepostulantes.stream().map(p -> {
            Map<String, Object> perfil = new LinkedHashMap<>();
            perfil.put("id",     p.getIdPrepostulacion());
            perfil.put("nombre", p.getNombres() + " " + p.getApellidos());
            perfil.put("correo", p.getCorreo());
            perfil.put("cedula", p.getIdentificacion());
            perfil.put("estado", p.getEstadoRevision());

            // Documentos subidos
            List<PrepostulacionDocumento> docs = documentoRepository.findByIdPrepostulacion(p.getIdPrepostulacion());
            List<String> descripciones = docs.stream()
                    .map(PrepostulacionDocumento::getDescripcion)
                    .collect(Collectors.toList());
            perfil.put("documentosSubidos", descripciones);
            perfil.put("cantidadDocumentos", docs.size());

            // ¿Tiene cédula y foto?
            perfil.put("tieneCedula", p.getUrlCedula() != null && !p.getUrlCedula().isBlank());
            perfil.put("tieneFoto",   p.getUrlFoto()   != null && !p.getUrlFoto().isBlank());

            return perfil;
        }).collect(Collectors.toList());

        // 5. Construir prompt para Mistral
        String prompt = buildPrompt(request, solicitud, requisitos, perfiles);

        // 6. Llamar a Mistral
        String respuestaJson = llamarMistral(prompt);

        // 7. Parsear respuesta y devolver DTO ordenado
        return parsearRespuesta(respuestaJson, prepostulantes);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONSTRUCCIÓN DEL PROMPT
    // ═════════════════════════════════════════════════════════════════════════

    private String buildPrompt(
            RankingPrepostulacionesRequestDTO request,
            SolicitudDocente solicitud,
            List<RequisitoPrepostulacion> requisitos,
            List<Map<String, Object>> perfiles) {

        StringBuilder sb = new StringBuilder();

        sb.append("Eres un asistente de evaluación académica universitaria. ");
        sb.append("Tu tarea es analizar pre-postulantes a una convocatoria docente ");
        sb.append("y generar un ranking objetivo basado en los criterios indicados.\n\n");

        // ── Criterios seleccionados por el revisor ────────────────────────────
        sb.append("=== CRITERIOS DE ANÁLISIS SELECCIONADOS ===\n");
        if (request.isAnalizarDocumentos()) {
            sb.append("- Completitud de documentos: comparar documentos subidos vs requeridos.\n");
        }
        if (request.isAnalizarNivelAcademico()) {
            sb.append("- Nivel académico: verificar si el nivel declarado cumple el mínimo requerido.\n");
        }
        sb.append("\n");

        // ── Requisitos de la solicitud ────────────────────────────────────────
        sb.append("=== REQUISITOS DE LA SOLICITUD (ID: ").append(solicitud.getIdSolicitud()).append(") ===\n");
        sb.append("Nivel académico requerido: ").append(solicitud.getNivelAcademico()).append("\n");
        sb.append("Materia: ").append(
                solicitud.getMateria() != null ? solicitud.getMateria().getNombreMateria() : "No especificada"
        ).append("\n");

        if (!requisitos.isEmpty()) {
            sb.append("Documentos requeridos (").append(requisitos.size()).append(" en total):\n");
            requisitos.forEach(r -> sb.append("  - ").append(r.getNombre())
                    .append(r.getDescripcion() != null ? " (" + r.getDescripcion() + ")" : "")
                    .append("\n"));
        } else {
            sb.append("Documentos requeridos: ninguno configurado explícitamente.\n");
        }
        sb.append("\n");

        // ── Jerarquía de niveles académicos ──────────────────────────────────
        sb.append("=== JERARQUÍA DE NIVELES ACADÉMICOS (de mayor a menor) ===\n");
        sb.append("PHD / DOCTORADO > MAESTRÍA > ESPECIALIZACIÓN > LICENCIATURA / INGENIERÍA > TECNÓLOGO / TECNOLOGÍA\n\n");

        // ── Perfiles de pre-postulantes ───────────────────────────────────────
        sb.append("=== PRE-POSTULANTES A EVALUAR ===\n");
        for (Map<String, Object> p : perfiles) {
            sb.append("--- Pre-postulante ID: ").append(p.get("id")).append(" ---\n");
            sb.append("Nombre: ").append(p.get("nombre")).append("\n");
            sb.append("Cédula: ").append(p.get("cedula")).append("\n");
            sb.append("Estado actual: ").append(p.get("estado")).append("\n");
            sb.append("Tiene cédula subida: ").append(p.get("tieneCedula")).append("\n");
            sb.append("Tiene foto subida: ").append(p.get("tieneFoto")).append("\n");

            @SuppressWarnings("unchecked")
            List<String> docs = (List<String>) p.get("documentosSubidos");
            sb.append("Documentos subidos (").append(p.get("cantidadDocumentos")).append("):\n");
            if (docs.isEmpty()) {
                sb.append("  (ninguno)\n");
            } else {
                docs.forEach(d -> sb.append("  - ").append(d).append("\n"));
            }
            sb.append("\n");
        }

        // ── Instrucción de respuesta ──────────────────────────────────────────
        sb.append("=== INSTRUCCIONES DE RESPUESTA ===\n");
        sb.append("Responde ÚNICAMENTE con un JSON válido. Sin texto adicional, sin markdown, sin bloques ```.\n");
        sb.append("El JSON debe tener este formato exacto:\n");
        sb.append("{\n");
        sb.append("  \"ranking\": [\n");
        sb.append("    {\n");
        sb.append("      \"id\": <número idPrepostulacion>,\n");
        sb.append("      \"posicion\": <1 para el mejor, 2 el siguiente, etc.>,\n");
        sb.append("      \"puntuacion\": <0 a 100, número entero>,\n");
        sb.append("      \"nivelCumplimiento\": <\"ALTO\", \"MEDIO\" o \"BAJO\">,\n");
        sb.append("      \"resumen\": \"<1-2 frases concisas sobre el perfil del candidato>\",\n");
        sb.append("      \"fortalezas\": [\"<fortaleza 1>\", \"<fortaleza 2>\"],\n");
        sb.append("      \"observaciones\": \"<observación sobre documentos faltantes o nivel académico>\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"resumenGeneral\": \"<párrafo breve sobre el conjunto de candidatos>\"\n");
        sb.append("}\n");
        sb.append("Ordena el array 'ranking' de mayor a menor puntuación (posicion 1 = mejor candidato).\n");
        sb.append("Sé objetivo, conciso y usa español.\n");

        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LLAMADA A MISTRAL API
    // ═════════════════════════════════════════════════════════════════════════

    private String llamarMistral(String prompt) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model",      MODELO,
                    "max_tokens", 3000,
                    "messages",   List.of(Map.of("role", "user", "content", prompt))
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MISTRAL_URL))
                    .header("Authorization", "Bearer " + mistralApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .build()) {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            }

            log.info("[MISTRAL] Respuesta HTTP: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("[MISTRAL] Error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Mistral respondió con error " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("choices").path(0).path("message").path("content").asText("{}").trim();

        } catch (Exception e) {
            log.error("[MISTRAL] Excepción al llamar API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al contactar Mistral AI: " + e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PARSEO DE RESPUESTA
    // ═════════════════════════════════════════════════════════════════════════

    private List<RankingResultadoDTO> parsearRespuesta(
            String jsonStr, List<Prepostulacion> prepostulantes) {

        // Mapa id → Prepostulacion para enriquecer con datos reales
        Map<Long, Prepostulacion> mapaPrepostulantes = prepostulantes.stream()
                .collect(Collectors.toMap(Prepostulacion::getIdPrepostulacion, p -> p));

        try {
            // Limpiar posibles bloques markdown que el modelo agregue
            String limpio = jsonStr
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode root        = objectMapper.readTree(limpio);
            JsonNode rankingNode = root.path("ranking");
            String resumenGeneral = root.path("resumenGeneral").asText("");

            List<RankingResultadoDTO> resultados = new ArrayList<>();

            for (JsonNode item : rankingNode) {
                Long id = item.path("id").asLong();
                Prepostulacion prepost = mapaPrepostulantes.get(id);

                // Construir fortalezas
                List<String> fortalezas = new ArrayList<>();
                for (JsonNode f : item.path("fortalezas")) {
                    fortalezas.add(f.asText());
                }

                RankingResultadoDTO dto = new RankingResultadoDTO();
                dto.setIdPrepostulacion(id);
                dto.setPosicion(item.path("posicion").asInt());
                dto.setPuntuacion(item.path("puntuacion").asInt());
                dto.setNivelCumplimiento(item.path("nivelCumplimiento").asText("MEDIO"));
                dto.setResumen(item.path("resumen").asText(""));
                dto.setFortalezas(fortalezas);
                dto.setObservaciones(item.path("observaciones").asText(""));
                dto.setResumenGeneral(resumenGeneral);

                // Datos reales del pre-postulante
                if (prepost != null) {
                    dto.setNombre(prepost.getNombres() + " " + prepost.getApellidos());
                    dto.setCorreo(prepost.getCorreo());
                    dto.setIdentificacion(prepost.getIdentificacion());
                    dto.setEstadoRevision(prepost.getEstadoRevision());
                }

                resultados.add(dto);
            }

            // Ordenar por posición por si Mistral no respetó el orden
            resultados.sort(Comparator.comparingInt(RankingResultadoDTO::getPosicion));
            return resultados;

        } catch (Exception e) {
            log.error("[MISTRAL] Error parseando JSON: {}", e.getMessage());
            log.error("[MISTRAL] JSON recibido: {}", jsonStr);
            throw new RuntimeException("No se pudo procesar la respuesta de Mistral AI. Inténtalo de nuevo.", e);
        }
    }
}