package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
@Data
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

}