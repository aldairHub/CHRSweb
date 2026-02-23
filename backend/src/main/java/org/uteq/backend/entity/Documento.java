package org.uteq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long idDocumento;

    // FK → postulacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    private Postulacion postulacion;

    // FK → tipo_documento (ya tienes la entity TipoDocumento)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    // FK → documento_temporal (opcional, puede ser null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_temporal")
    private DocumentoTemporal documentoTemporal;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    // valores: pendiente | validado | rechazado
    @Column(name = "estado_validacion", nullable = false)
    private String estadoValidacion = "pendiente";

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    // Getters y Setters
    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long v) { this.idDocumento = v; }

    public Postulacion getPostulacion() { return postulacion; }
    public void setPostulacion(Postulacion v) { this.postulacion = v; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento v) { this.tipoDocumento = v; }

    public DocumentoTemporal getDocumentoTemporal() { return documentoTemporal; }
    public void setDocumentoTemporal(DocumentoTemporal v) { this.documentoTemporal = v; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime v) { this.fechaCarga = v; }

    public String getEstadoValidacion() { return estadoValidacion; }
    public void setEstadoValidacion(String v) { this.estadoValidacion = v; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String v) { this.rutaArchivo = v; }
}