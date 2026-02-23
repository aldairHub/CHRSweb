package org.uteq.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resultados_ia_documento")
public class ResultadosIaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado_ia_documento")
    private Long idResultadoIaDocumento;

    // FK → documento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    @Column(name = "resultado", nullable = false)
    private String resultado;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "fecha_revision", nullable = false)
    private LocalDateTime fechaRevision;

    // Getters y Setters
    public Long getIdResultadoIaDocumento() { return idResultadoIaDocumento; }
    public void setIdResultadoIaDocumento(Long v) { this.idResultadoIaDocumento = v; }

    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento v) { this.documento = v; }

    public String getResultado() { return resultado; }
    public void setResultado(String v) { this.resultado = v; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }

    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime v) { this.fechaRevision = v; }
}