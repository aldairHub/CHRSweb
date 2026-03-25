package org.uteq.backend.dto;

import lombok.Data;

/**
 * DTO que recibe el frontend con los criterios elegidos en el modal de análisis IA.
 */
@Data
public class RankingPrepostulacionesRequestDTO {

    /** ID de la solicitud docente a analizar */
    private Long idSolicitud;

    /** Analizar completitud de documentos subidos vs requeridos */
    private boolean analizarDocumentos = true;

    /** Analizar nivel académico del pre-postulante vs el requerido */
    private boolean analizarNivelAcademico = true;
}