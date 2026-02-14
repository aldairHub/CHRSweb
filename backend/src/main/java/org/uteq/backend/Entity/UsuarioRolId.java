package org.uteq.backend.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data
@Embeddable
public class UsuarioRolId implements Serializable {

    @Column(name="id_usuario")
    private Long idUsuario;

    @Column(name="id_rol_usuario")
    private Long idRolUsuario;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRolId that)) return false;
        return java.util.Objects.equals(idUsuario, that.idUsuario)
                && java.util.Objects.equals(idRolUsuario, that.idRolUsuario);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(idUsuario, idRolUsuario);
    }
}
