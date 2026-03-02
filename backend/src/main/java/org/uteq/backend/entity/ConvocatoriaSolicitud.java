package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "convocatoria_solicitud")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConvocatoriaSolicitud {

    @EmbeddedId
    private ConvocatoriaSolicitudId id = new ConvocatoriaSolicitudId();

    // Getters de conveniencia para que el controller siga funcionando igual
    public Long getIdConvocatoria() { return id.getIdConvocatoria(); }
    public Long getIdSolicitud()    { return id.getIdSolicitud(); }

    public void setIdConvocatoria(Long idConvocatoria) { id.setIdConvocatoria(idConvocatoria); }
    public void setIdSolicitud(Long idSolicitud)       { id.setIdSolicitud(idSolicitud); }

    // ── PK compuesta embebida ──────────────────────────────────────────────
    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class ConvocatoriaSolicitudId implements Serializable {
        @Column(name = "id_convocatoria") private Long idConvocatoria;
        @Column(name = "id_solicitud")    private Long idSolicitud;
    }
}