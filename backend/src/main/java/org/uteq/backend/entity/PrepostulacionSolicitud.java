package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prepostulacion_solicitud", schema = "public")
@Data
@NoArgsConstructor
public class PrepostulacionSolicitud {

    @EmbeddedId
    private PrepostulacionSolicitudId id;

    public PrepostulacionSolicitud(Long idPrepostulacion, Long idSolicitud) {
        this.id = new PrepostulacionSolicitudId(idPrepostulacion, idSolicitud);
    }
}