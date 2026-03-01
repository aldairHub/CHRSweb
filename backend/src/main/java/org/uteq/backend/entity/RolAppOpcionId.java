package org.uteq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;

@Embeddable
@Data
public class RolAppOpcionId implements Serializable {

    @Column(name = "id_rol_app")
    private Integer idRolApp;

    @Column(name = "id_opcion")
    private Integer idOpcion;
}
