package org.uteq.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class UsuarioRolId implements Serializable {
    @Column(name="id_usuario")
    private Long idUsuario;

    @Column(name="id_rol")
    private Long idRol;
}
