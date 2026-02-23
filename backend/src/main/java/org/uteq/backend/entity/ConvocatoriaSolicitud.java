package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "convocatoria_solicitud")
@Data
public class ConvocatoriaSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_convocatoria")
    private Long idConvocatoria;

    @Column(name = "id_solicitud")
    private Long idSolicitud;
}