package org.uteq.backend.dto;

// ============================================================
// DTOs — Módulo: Subir Documentos
// ============================================================

// DTO de respuesta para un documento (viene del SP 1)
public class DocumentoResponseDTO {

    private Long idTipoDocumento;
    private String nombreTipo;
    private Boolean obligatorio;
    private Long idDocumento;
    private String estadoValidacion;  // pendiente | validado | rechazado
    private String rutaArchivo;
    private String fechaCarga;
    private String observacionesIa;

    // Getters y Setters
    public Long getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Long v) { this.idTipoDocumento = v; }

    public String getNombreTipo() { return nombreTipo; }
    public void setNombreTipo(String v) { this.nombreTipo = v; }

    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean v) { this.obligatorio = v; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long v) { this.idDocumento = v; }

    public String getEstadoValidacion() { return estadoValidacion; }
    public void setEstadoValidacion(String v) { this.estadoValidacion = v; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String v) { this.rutaArchivo = v; }

    public String getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(String v) { this.fechaCarga = v; }

    public String getObservacionesIa() { return observacionesIa; }
    public void setObservacionesIa(String v) { this.observacionesIa = v; }
}