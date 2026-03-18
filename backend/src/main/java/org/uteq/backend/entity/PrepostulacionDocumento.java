package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "prepostulacion_documentos")
@Data
public class PrepostulacionDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long idDocumento;

    @Column(name = "id_prepostulacion", nullable = false)
    private Long idPrepostulacion;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "url_documento", nullable = false)
    private String urlDocumento;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    /** NULL = documento libre, NOT NULL = requisito obligatorio de la solicitud */
    @Column(name = "id_requisito")
    private Long idRequisito;

    public PrepostulacionDocumento() {
        this.fechaSubida = LocalDateTime.now();
    }
}
