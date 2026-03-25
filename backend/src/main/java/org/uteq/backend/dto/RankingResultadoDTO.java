package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO de resultado del ranking para un pre-postulante individual.
 */
@Data
public class RankingResultadoDTO {

    // ── Identificación ────────────────────────────────────────────────────────
    private Long   idPrepostulacion;
    private String nombre;
    private String correo;
    private String identificacion;
    private String estadoRevision;

    // ── Resultado IA ─────────────────────────────────────────────────────────
    /** Posición en el ranking (1 = mejor candidato) */
    private int    posicion;

    /** Puntuación de 0 a 100 */
    private int    puntuacion;

    /** ALTO | MEDIO | BAJO */
    private String nivelCumplimiento;

    /** Resumen corto del perfil */
    private String resumen;

    /** Lista de fortalezas identificadas */
    private List<String> fortalezas;

    /** Observaciones sobre documentos o nivel académico */
    private String observaciones;

    /** Párrafo general sobre el conjunto (solo se incluye en el primer elemento) */
    private String resumenGeneral;
}