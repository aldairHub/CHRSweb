package org.uteq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrepostulacionSolicitudId {
    @Column(name = "id_prepostulacion")
    private Long idPrepostulacion;

    @Column(name = "id_solicitud")
    private Long idSolicitud;
}
