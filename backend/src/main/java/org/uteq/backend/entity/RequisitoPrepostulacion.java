package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "requisito_prepostulacion")
@Data
public class RequisitoPrepostulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requisito")
    private Long idRequisito;

    @Column(name = "id_solicitud", nullable = false)
    private Long idSolicitud;

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
