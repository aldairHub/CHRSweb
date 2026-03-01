package org.uteq.backend.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "rol_app_opcion", schema = "public")
@Data
public class RolAppOpcion {

    @EmbeddedId
    private RolAppOpcionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolApp")
    @JoinColumn(name = "id_rol_app")
    private RolApp rolApp;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idOpcion")
    @JoinColumn(name = "id_opcion")
    private Opcion opcion;

    /**
     * Si true, el frontend oculta botones de crear/editar/eliminar
     * en la pantalla de esta opción.
     * La seguridad real la maneja el rol de BD.
     */
    @Column(name = "solo_lectura", nullable = false)
    private Boolean soloLectura = false;
}
