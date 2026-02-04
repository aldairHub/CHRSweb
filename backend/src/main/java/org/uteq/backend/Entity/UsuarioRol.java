package org.uteq.backend.Entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name="usuario_rol")
public class UsuarioRol {
    @EmbeddedId
    private UsuarioRolId id;

    @ManyToOne
    @MapsId("idUsuario")
    @JoinColumn(name="id_usuario")
    private Usuario usuario;

    @ManyToOne
    @MapsId("idRol")
    @JoinColumn(name="id_rol")
    private RolUsuario rol;
}
