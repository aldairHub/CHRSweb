package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

// ─── Detalle completo (vista panel lateral) ────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulanteDetalleDTO {

    private Long idPostulante;
    private String codigo;
    private String nombres;
    private String apellidos;
    private String cedula;
    private String materia;
    private String faseActual;
    private Integer progreso;
    private String estadoGeneral;

    private List<FaseProcesoDTO> fases;
    private List<HistorialAccionDTO> historial;

    // ─── Inner DTOs ────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaseProcesoDTO {
        private Long idFase;
        private Integer orden;
        private String nombre;
        private Integer peso;
        /** completada | en_curso | pendiente | bloqueada */
        private String estado;
        private Double calificacion;
        private String fechaCompletada;
        private ReunionResumenDTO reunion;
        private List<String> evaluadores;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReunionResumenDTO {
        private Long idReunion;
        private Long idPostulante;
        private Long idFase;
        private String fecha;
        private String hora;
        private Integer duracion;
        private String modalidad;
        private String enlace;
        private List<String> evaluadores;
        private String observaciones;
        /** programada | en_curso | completada | cancelada */
        private String estado;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistorialAccionDTO {
        private String fecha;
        private String titulo;
        private String descripcion;
        private String usuario;
    }
}
