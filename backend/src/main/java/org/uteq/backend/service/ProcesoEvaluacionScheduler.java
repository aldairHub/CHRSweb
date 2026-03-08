package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.entity.Postulante;
import org.uteq.backend.entity.SolicitudDocente;
import org.uteq.backend.repository.PostulanteRepository;
import org.uteq.backend.repository.SolicitudDocenteRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcesoEvaluacionScheduler {

    private final JdbcTemplate jdbc;
    private final ProcesoEvaluacionService procesoService;
    private final PostulanteRepository postulanteRepository;
    private final SolicitudDocenteRepository solicitudRepository;


    @Scheduled(cron = "0 0 0 * * *") // Cada día a medianoche
    @Transactional
    public void iniciarProcesosAutomaticos() {
        log.info("=== Scheduler: Verificando postulantes para iniciar proceso de evaluación ===");

        String sql = """
                SELECT p.id_postulante, prs.id_solicitud
                FROM postulante p
                JOIN prepostulacion pre ON p.id_prepostulacion = pre.id_prepostulacion
                JOIN prepostulacion_solicitud prs ON pre.id_prepostulacion = prs.id_prepostulacion
                JOIN convocatoria_solicitud cs ON prs.id_solicitud = cs.id_solicitud
                JOIN convocatoria c ON cs.id_convocatoria = c.id_convocatoria
                WHERE pre.estado_revision = 'APROBADO'
                AND c.fecha_limite_documentos = CURRENT_DATE
                AND NOT EXISTS (
                    SELECT 1 FROM proceso_evaluacion pe
                    WHERE pe.id_postulante = p.id_postulante
                    AND pe.id_solicitud = prs.id_solicitud
                )
                """;

        List<Map<String, Object>> postulantes = jdbc.queryForList(sql);

        if (postulantes.isEmpty()) {
            log.info("Scheduler: No hay postulantes para iniciar proceso hoy.");
            return;
        }

        log.info("Scheduler: {} postulante(s) encontrados para iniciar proceso.", postulantes.size());

        int exitosos = 0;
        int fallidos = 0;

        for (Map<String, Object> row : postulantes) {
            Long idPostulante = ((Number) row.get("id_postulante")).longValue();
            Long idSolicitud  = ((Number) row.get("id_solicitud")).longValue();

            try {
                Postulante postulante = postulanteRepository.findById(idPostulante)
                        .orElseThrow(() -> new RuntimeException("Postulante no encontrado: " + idPostulante));

                SolicitudDocente solicitud = solicitudRepository.findById(idSolicitud)
                        .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + idSolicitud));

                procesoService.iniciarProceso(postulante, solicitud);
                log.info("Scheduler: Proceso iniciado para postulante {} - solicitud {}", idPostulante, idSolicitud);
                exitosos++;

            } catch (Exception e) {
                log.error("Scheduler: Error al iniciar proceso para postulante {} - solicitud {}: {}",
                        idPostulante, idSolicitud, e.getMessage());
                fallidos++;
            }
        }

        log.info("Scheduler: Completado. Exitosos: {}, Fallidos: {}", exitosos, fallidos);
    }
}