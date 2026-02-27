package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
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

    // FK → tipo_documento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    // FK → documento_temporal
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

}