package org.uteq.backend.dto;

// DTO para TipoDocumento
public class TipoDocumentoDTO {

    private Long idTipoDocumento;
    private String nombre;
    private String descripcion;
    private Boolean obligatorio;
    private Boolean activo;

    public Long getIdTipoDocumento() { return idTipoDocumento; }
    public void setIdTipoDocumento(Long v) { this.idTipoDocumento = v; }

    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }

    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean v) { this.obligatorio = v; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean v) { this.activo = v; }
}