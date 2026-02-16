package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documento_temporal")
public class DocumentoTemporal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_temporal")
    private Long idDocumentoTemporal;

    @Column(name = "id_prepostulacion", nullable = false)
    private Long idPrepostulacion;

    @Column(name = "id_tipo_documento", nullable = false)
    private Long idTipoDocumento;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();

    @Column(name = "ruta_archivo", nullable = false, columnDefinition = "TEXT")
    private String rutaArchivo;

    @Column(name = "nombre_original")
    private String nombreOriginal;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;
}