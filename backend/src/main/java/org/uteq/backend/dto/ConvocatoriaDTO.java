package org.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

public class ConvocatoriaDTO {

    // ─────────────────────────────────────
    // REQUESTS
    // ─────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CrearRequest {
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
        private List<Long> idsSolicitudes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ActualizarRequest {
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CambiarEstadoRequest {
        private String nuevoEstado; // 'abierta' | 'cerrada' | 'cancelada'
    }

    // ─────────────────────────────────────
    // RESPONSES
    // ─────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ListaResponse {
        private Long      idConvocatoria;
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
        private String    estadoConvocatoria;
        private Long      totalSolicitudes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DetalleResponse {
        private Long      idConvocatoria;
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
        private String    estadoConvocatoria;
        private List<SolicitudResumen> solicitudes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SolicitudResumen {
        private Long   idSolicitud;
        private String nombreMateria;
        private String nombreCarrera;
        private String nombreFacultad;
        private Long   cantidadDocentes;
        private String nivelAcademico;
        private String estadoSolicitud;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MensajeResponse {
        private boolean exito;
        private String  mensaje;
        private Object  data;
    }
}