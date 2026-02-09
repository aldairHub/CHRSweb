package org.uteq.backend.Entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name="usuario_rol")
public class UsuarioRol {
    @EmbeddedId
    private UsuarioRolId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name="id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRolUsuario")
    @JoinColumn(name="id_rol_usuario", nullable = false)
    private RolUsuario rol;
}
