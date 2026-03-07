package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "convocatoria_tipo_documento",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conv_tipodoc",
                columnNames = {"id_convocatoria", "id_tipo_documento"}
        )
)
public class ConvocatoriaTipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_convocatoria", nullable = false)
    private Convocatoria convocatoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio;

}