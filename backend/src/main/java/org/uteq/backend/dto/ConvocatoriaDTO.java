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
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaLimiteDocumentos; // NUEVO
        private List<Long> idsSolicitudes;
        private List<Long> idsTiposDocumento;  // NUEVO
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ActualizarRequest {
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaLimiteDocumentos; // NUEVO
        private List<Long> idsTiposDocumento;  // NUEVO
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
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaLimiteDocumentos; // NUEVO
        private String    estadoConvocatoria;
        private Long      totalSolicitudes;
        private String    imagenPortadaUrl;
        private boolean   documentosAbiertos;  // NUEVO
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DetalleResponse {
        private Long      idConvocatoria;
        private String    titulo;
        private String    descripcion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaPublicacion;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaInicio;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaFin;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate fechaLimiteDocumentos; // NUEVO
        private String    estadoConvocatoria;
        private String    imagenPortadaUrl;
        private boolean   documentosAbiertos;         // NUEVO
        private List<SolicitudResumen>    solicitudes;
        private List<TipoDocumentoConv>   tiposDocumento; // NUEVO
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
    public static class TipoDocumentoConv {  // NUEVO
        private Long    idTipoDocumento;
        private String  nombre;
        private String  descripcion;
        private boolean obligatorio;
        private String  fuente; // 'convocatoria' | 'global'
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MensajeResponse {
        private boolean exito;
        private String  mensaje;
        private Object  data;
    }
}